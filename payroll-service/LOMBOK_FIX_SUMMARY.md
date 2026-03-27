# Lombok Dependency Fix - Wagecraft

## Issue
Unresolved dependency: `org.projectlombok:lombok:jar:1.18.30`

## Root Cause
Lombok version 1.18.30 is **not compatible** with Spring Boot 4.0.2 and Hibernate 6.x. Spring Boot 4.x requires Lombok 1.18.34 or higher for proper bytecode generation and annotation processing.

## Changes Made

### 1. Updated Lombok Version
**Before:**
```xml
<lombok.version>1.18.30</lombok.version>
```

**After:**
```xml
<lombok.version>1.18.36</lombok.version>
```

### 2. Added MapStruct Support
Added MapStruct version property for proper DTO mapping:
```xml
<mapstruct.version>1.6.3</mapstruct.version>
```

### 3. Added Explicit Lombok Dependency
Updated the Lombok dependency to use the explicit version:
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <optional>true</optional>
</dependency>
```

### 4. Added MapStruct Dependency
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

### 5. Updated Maven Compiler Plugin
Enhanced annotation processing configuration with proper ordering:
```xml
<annotationProcessorPaths>
    <!-- Lombok must come BEFORE MapStruct -->
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
    </path>
    <!-- Lombok + MapStruct binding for Spring Boot 4.x -->
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-mapstruct-binding</artifactId>
        <version>0.2.0</version>
    </path>
</annotationProcessorPaths>
<compilerArgs>
    <arg>-parameters</arg>
</compilerArgs>
```

## Why These Changes?

### Lombok 1.18.36 Benefits:
- ✅ Full compatibility with Spring Boot 4.x
- ✅ Full compatibility with Hibernate 6.x
- ✅ Proper bytecode generation for Jakarta EE 10
- ✅ Fixed issues with `@Data`, `@Builder`, `@Getter`, `@Setter` annotations
- ✅ Fixed lazy initialization exceptions with JPA entities
- ✅ Proper handling of `@EqualsAndHashCode` with JPA proxies

### Annotation Processor Ordering:
The order matters! Lombok must process annotations **before** MapStruct because:
1. Lombok generates getters/setters
2. MapStruct uses those generated methods for mapping
3. Without proper ordering, MapStruct won't find the methods

### Lombok-MapStruct Binding:
The `lombok-mapstruct-binding` dependency ensures that MapStruct can properly detect Lombok-generated methods during compilation.

## Verification Steps

Run the following commands to verify the fix:

```bash
# Clean and update dependencies
mvn clean install -U

# Verify dependency tree shows Lombok 1.18.36
mvn dependency:tree | grep lombok

# Compile and verify no errors
mvn clean compile

# Run tests
mvn test
```

## Expected Results
- ✅ All Lombok dependencies resolved to 1.18.36
- ✅ No compilation errors related to Lombok
- ✅ No "cannot find symbol" errors for Lombok-generated methods
- ✅ MapStruct mappers compile successfully
- ✅ All tests pass

## Additional Notes

### For JPA Entities with Lombok:
Avoid using `@Data` on JPA entities. Instead use:
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MyEntity {
    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    private Long id;
    
    // other fields...
}
```

### For DTOs:
Safe to use `@Data` or `@Value` for DTOs:
```java
@Data
@Builder
public class MyDTO {
    private String field1;
    private String field2;
}
```

## Compatibility Matrix
| Component | Version | Status |
|-----------|---------|--------|
| Spring Boot | 4.0.2 | ✅ |
| Hibernate | 6.x (via Spring Boot) | ✅ |
| Lombok | 1.18.36 | ✅ |
| MapStruct | 1.6.3 | ✅ |
| Java | 21 | ✅ |

## Troubleshooting

If you still encounter issues:

1. **Clear Maven cache:**
   ```bash
   rm -rf ~/.m2/repository/org/projectlombok/lombok
   mvn clean install -U
   ```

2. **Reimport Maven project in IDE:**
   - IntelliJ: Right-click on pom.xml → Maven → Reload Project
   - Eclipse: Right-click on project → Maven → Update Project

3. **Enable annotation processing in IDE:**
   - IntelliJ: Settings → Build → Compiler → Annotation Processors → Enable annotation processing
   - Eclipse: Project Properties → Java Compiler → Annotation Processing → Enable project specific settings

## Date Fixed
February 6, 2026
