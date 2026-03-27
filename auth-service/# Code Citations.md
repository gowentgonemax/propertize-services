# Code Citations

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````

## License: unknown

https://github.com/Tachiguishi/Tachiguishi.github.io/blob/d9ae1684a02b1698bceb47339a69fe613d58a28e/_drafts/TBD.md

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>

````

## License: unknown

https://github.com/sachinmud/EmployeeMaster/blob/29b0834a4948c1509bdd5d03929c0ecc2a9d7553/.metadata/.plugins/org.eclipse.core.resources/.history/50/d0168fcf4771001b11b5dbf027a22b82

````
Build passes. Here's what happened and what was fixed:

**Root cause:** Spring Boot 3.5.10 uses `maven-compiler-plugin` **3.14.1**, which changed the default behavior — annotation processors are **no longer automatically discovered** from the compile classpath. Lombok (declared as a `<dependency>`) was silently not running, so `@Slf4j`, `@Data`, `@Builder`, `@Getter` etc. generated no code, causing 100 "cannot find symbol" errors across all classes using those annotations.

**Fix applied** to [auth-service/pom.xml](auth-service/pom.xml): Added explicit `annotationProcessorPaths` to the `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
````
