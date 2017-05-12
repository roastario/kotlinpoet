/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import java.util.Collections
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.lang.model.element.Modifier

class KotlinFileTest {
  @Test fun importStaticReadmeExample() {
    val hoverboard = ClassName.get("com.mattel", "Hoverboard")
    val namedBoards = ClassName.get("com.mattel", "Hoverboard", "Boards")
    val list = ClassName.get("java.util", "List")
    val arrayList = ClassName.get("java.util", "ArrayList")
    val listOfHoverboards = ParameterizedTypeName.get(list, hoverboard)
    val beyond = FunSpec.builder("beyond")
        .returns(listOfHoverboards)
        .addStatement("%T result = new %T<>()", listOfHoverboards, arrayList)
        .addStatement("result.add(%T.createNimbus(2000))", hoverboard)
        .addStatement("result.add(%T.createNimbus(\"2001\"))", hoverboard)
        .addStatement("result.add(%T.createNimbus(%T.THUNDERBOLT))", hoverboard, namedBoards)
        .addStatement("%T.sort(result)", Collections::class.java)
        .addStatement("return result.isEmpty() ? %T.emptyList() : result", Collections::class.java)
        .build()
    val hello = TypeSpec.classBuilder("HelloWorld")
        .addFun(beyond)
        .build()
    val source = KotlinFile.builder("com.example.helloworld", hello)
        .addStaticImport(hoverboard, "createNimbus")
        .addStaticImport(namedBoards, "*")
        .addStaticImport(Collections::class.java, "*")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.example.helloworld
        |
        |import static com.mattel.Hoverboard.Boards.*
        |import static com.mattel.Hoverboard.createNimbus
        |import static java.util.Collections.*
        |
        |import com.mattel.Hoverboard
        |import java.util.ArrayList
        |import java.util.List
        |
        |class HelloWorld {
        |  fun beyond(): List<Hoverboard> {
        |    List<Hoverboard> result = new ArrayList<>()
        |    result.add(createNimbus(2000))
        |    result.add(createNimbus("2001"))
        |    result.add(createNimbus(THUNDERBOLT))
        |    sort(result)
        |    return result.isEmpty() ? emptyList() : result
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticForCrazyFormatsWorks() {
    val method = FunSpec.builder("method").build()
    KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addStaticBlock(CodeBlock.builder()
                .addStatement("%T", Runtime::class.java)
                .addStatement("%T.a()", Runtime::class.java)
                .addStatement("%T.X", Runtime::class.java)
                .addStatement("%T%T", Runtime::class.java, Runtime::class.java)
                .addStatement("%T.%T", Runtime::class.java, Runtime::class.java)
                .addStatement("%1T%1T", Runtime::class.java)
                .addStatement("%1T%2L%1T", Runtime::class.java, "?")
                .addStatement("%1T%2L%2S%1T", Runtime::class.java, "?")
                .addStatement("%1T%2L%2S%1T%3N%1T", Runtime::class.java, "?", method)
                .addStatement("%T%L", Runtime::class.java, "?")
                .addStatement("%T%S", Runtime::class.java, "?")
                .addStatement("%T%N", Runtime::class.java, method)
                .build())
            .build())
        .addStaticImport(Runtime::class.java, "*")
        .build()
        .toString() // don't look at the generated code...
  }

  @Test fun importStaticMixed() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addStaticBlock(CodeBlock.builder()
                .addStatement("assert %1T.valueOf(\"BLOCKED\") == %1T.BLOCKED", Thread.State::class.java)
                .addStatement("%T.gc()", System::class.java)
                .addStatement("%1T.out.println(%1T.nanoTime())", System::class.java)
                .build())
            .addFun(FunSpec.constructorBuilder()
                .addParameter(Array<Thread.State>::class.java, "states")
                .varargs(true)
                .build())
            .build())
        .addStaticImport(Thread.State.BLOCKED)
        .addStaticImport(System::class.java, "*")
        .addStaticImport(Thread.State::class.java, "valueOf")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import static java.lang.System.*
        |import static java.lang.Thread.State.BLOCKED
        |import static java.lang.Thread.State.valueOf
        |
        |import java.lang.Thread
        |
        |class Taco {
        |  static {
        |    assert valueOf("BLOCKED") == BLOCKED
        |    gc()
        |    out.println(nanoTime())
        |  }
        |
        |  constructor(vararg states: Thread.State) {
        |  }
        |}
        |""".trimMargin())
  }

  @Ignore("addStaticImport doesn't support members with %L")
  @Test
  fun importStaticDynamic() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addFun(FunSpec.builder("main")
                .addStatement("%T.%L.println(%S)", System::class.java, "out", "hello")
                .build())
            .build())
        .addStaticImport(System::class.java, "out")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos;
        |
        |import static java.lang.System.out;
        |
        |class Taco {
        |  void main() {
        |    out.println("hello");
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticNone() {
    val source = KotlinFile.builder("readme", importStaticTypeSpec("Util")).build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import java.lang.System
        |import java.util.concurrent.TimeUnit
        |import kotlin.Long
        |
        |class Util {
        |  public static fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return TimeUnit.SECONDS.convert(minutes, TimeUnit.MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticOnce() {
    val source = KotlinFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS).build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import static java.util.concurrent.TimeUnit.SECONDS
        |
        |import java.lang.System
        |import java.util.concurrent.TimeUnit
        |import kotlin.Long
        |
        |class Util {
        |  public static fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return SECONDS.convert(minutes, TimeUnit.MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticTwice() {
    val source = KotlinFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit.SECONDS)
        .addStaticImport(TimeUnit.MINUTES)
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import static java.util.concurrent.TimeUnit.MINUTES
        |import static java.util.concurrent.TimeUnit.SECONDS
        |
        |import java.lang.System
        |import kotlin.Long
        |
        |class Util {
        |  public static fun minutesToSeconds(minutes: Long): Long {
        |    System.gc()
        |    return SECONDS.convert(minutes, MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun importStaticUsingWildcards() {
    val source = KotlinFile.builder("readme", importStaticTypeSpec("Util"))
        .addStaticImport(TimeUnit::class.java, "*")
        .addStaticImport(System::class.java, "*")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package readme
        |
        |import static java.lang.System.*
        |import static java.util.concurrent.TimeUnit.*
        |
        |import kotlin.Long
        |
        |class Util {
        |  public static fun minutesToSeconds(minutes: Long): Long {
        |    gc()
        |    return SECONDS.convert(minutes, MINUTES)
        |  }
        |}
        |""".trimMargin())
  }

  private fun importStaticTypeSpec(name: String): TypeSpec {
    val funSpec = FunSpec.builder("minutesToSeconds")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(Long::class.javaPrimitiveType!!)
        .addParameter(Long::class.javaPrimitiveType!!, "minutes")
        .addStatement("%T.gc()", System::class.java)
        .addStatement("return %1T.SECONDS.convert(minutes, %1T.MINUTES)", TimeUnit::class.java)
        .build()
    return TypeSpec.classBuilder(name).addFun(funSpec).build()

  }

  @Test fun noImports() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |}
        |""".trimMargin())
  }

  @Test fun singleImport() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addProperty(Date::class.java, "madeFreshDate")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.Date
        |
        |class Taco {
        |  madeFreshDate: Date;
        |}
        |""".trimMargin())
  }

  @Test fun conflictingImports() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addProperty(Date::class.java, "madeFreshDate")
            .addProperty(ClassName.get("java.sql", "Date"), "madeFreshDatabaseDate")
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import java.util.Date
        |
        |class Taco {
        |  madeFreshDate: Date;
        |
        |  madeFreshDatabaseDate: java.sql.Date;
        |}
        |""".trimMargin())
  }

  @Test fun skipJavaLangImportsWithConflictingClassLast() {
    // Whatever is used first wins! In this case the Float in java.lang is imported.
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addProperty(ClassName.get("java.lang", "Float"), "litres")
            .addProperty(ClassName.get("com.squareup.soda", "Float"), "beverage")
            .build())
        .skipJavaLangImports(true)
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  litres: Float;
        |
        |  beverage: com.squareup.soda.Float;
        |}
        |""".trimMargin())
  }

  @Test fun skipJavaLangImportsWithConflictingClassFirst() {
    // Whatever is used first wins! In this case the Float in com.squareup.soda is imported.
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addProperty(ClassName.get("com.squareup.soda", "Float"), "beverage")
            .addProperty(ClassName.get("java.lang", "Float"), "litres")
            .build())
        .skipJavaLangImports(true)
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.soda.Float
        |
        |class Taco {
        |  beverage: Float;
        |
        |  litres: java.lang.Float;
        |}
        |""".trimMargin())
  }

  @Test fun conflictingParentName() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("Twin").build())
                .addType(TypeSpec.classBuilder("C")
                    .addProperty(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class A {
        |  class B {
        |    class Twin {
        |    }
        |
        |    class C {
        |      d: A.Twin.D;
        |    }
        |  }
        |
        |  class Twin {
        |    class D {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun conflictingChildName() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addProperty(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .addType(TypeSpec.classBuilder("Twin").build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class A {
        |  class B {
        |    class C {
        |      d: A.Twin.D;
        |
        |      class Twin {
        |      }
        |    }
        |  }
        |
        |  class Twin {
        |    class D {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun conflictingNameOutOfScope() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("A")
            .addType(TypeSpec.classBuilder("B")
                .addType(TypeSpec.classBuilder("C")
                    .addProperty(ClassName.get("com.squareup.tacos", "A", "Twin", "D"), "d")
                    .addType(TypeSpec.classBuilder("Nested")
                        .addType(TypeSpec.classBuilder("Twin").build())
                        .build())
                    .build())
                .build())
            .addType(TypeSpec.classBuilder("Twin")
                .addType(TypeSpec.classBuilder("D")
                    .build())
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class A {
        |  class B {
        |    class C {
        |      d: Twin.D;
        |
        |      class Nested {
        |        class Twin {
        |        }
        |      }
        |    }
        |  }
        |
        |  class Twin {
        |    class D {
        |    }
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun nestedClassAndSuperclassShareName() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .superclass(ClassName.get("com.squareup.wire", "Message"))
            .addType(TypeSpec.classBuilder("Builder")
                .superclass(ClassName.get("com.squareup.wire", "Message", "Builder"))
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import com.squareup.wire.Message
        |
        |class Taco extends Message {
        |  class Builder extends Message.Builder {
        |  }
        |}
        |""".trimMargin())
  }

  /** https://github.com/square/javapoet/issues/366  */
  @Test fun annotationIsNestedClass() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("TestComponent")
            .addAnnotation(ClassName.get("dagger", "Component"))
            .addType(TypeSpec.classBuilder("Builder")
                .addAnnotation(ClassName.get("dagger", "Component", "Builder"))
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |import dagger.Component
        |
        |@Component
        |class TestComponent {
        |  @Component.Builder
        |  class Builder {
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun defaultPackage() {
    val source = KotlinFile.builder("",
        TypeSpec.classBuilder("HelloWorld")
            .addFun(FunSpec.builder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Array<String>::class.java, "args")
                .addCode("%T.out.println(%S);\n", System::class.java, "Hello World!")
                .build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |import java.lang.String
        |import java.lang.System
        |import kotlin.Array
        |
        |class HelloWorld {
        |  public static fun main(args: Array<String>) {
        |    System.out.println("Hello World!");
        |  }
        |}
        |""".trimMargin())
  }

  @Test fun defaultPackageTypesAreNotImported() {
    val source = KotlinFile.builder("hello",
        TypeSpec.classBuilder("World").addSuperinterface(ClassName.get("", "Test")).build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package hello
        |
        |class World implements Test {
        |}
        |""".trimMargin())
  }

  @Test fun topOfFileComment() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("Generated %L by KotlinPoet. DO NOT EDIT!", "2015-01-13")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |// Generated 2015-01-13 by KotlinPoet. DO NOT EDIT!
        |package com.squareup.tacos
        |
        |class Taco {
        |}
        |""".trimMargin())
  }

  @Test fun emptyLinesInTopOfFileComment() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco").build())
        .addFileComment("\nGENERATED FILE:\n\nDO NOT EDIT!\n")
        .build()
    assertThat(source.toString()).isEqualTo("""
        |//
        |// GENERATED FILE:
        |//
        |// DO NOT EDIT!
        |//
        |package com.squareup.tacos
        |
        |class Taco {
        |}
        |""".trimMargin())
  }

  @Test fun packageClassConflictsWithNestedClass() {
    val source = KotlinFile.builder("com.squareup.tacos",
        TypeSpec.classBuilder("Taco")
            .addProperty(ClassName.get("com.squareup.tacos", "A"), "a")
            .addType(TypeSpec.classBuilder("A").build())
            .build())
        .build()
    assertThat(source.toString()).isEqualTo("""
        |package com.squareup.tacos
        |
        |class Taco {
        |  a: com.squareup.tacos.A;
        |
        |  class A {
        |  }
        |}
        |""".trimMargin())
  }
}
