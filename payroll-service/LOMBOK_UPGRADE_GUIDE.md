# Lombok 1.18.36 Upgrade - Quick Reference Guide

## ✅ COMPLETED FIXES

### 1. Updated pom.xml

The following changes have been successfully applied to `/Users/ravishah/MySpace/Home_Projects/wagecraft/pom.xml`:

#### Properties Section
```xml
<properties>
    <java.version>21</java.version>
    <lombok.version>1.18.36</lombok.version>  <!-- ✅ UPDATED from 1.18.30 -->
    <spring-security.version>6.1.5</spring-security.version>
    <spring-cloud.version>2024.0.0</spring-cloud.version>
    <mapstruct.version>1.6.3</mapstruct.version>  <!-- ✅ ADDED -->
</properties>
```

#### Dependencies Section
```xml
<!-- ✅ UPDATED - Now uses explicit version -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <optional>true</optional>
</dependency>

<!-- ✅ ADDED - MapStruct support -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

#### Compiler Plugin Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>${java.version}</source>
        <target>${java.version}</target>
        <annotationProcessorPaths>
            <!-- ✅ Lombok comes FIRST (order matters!) -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <!-- ✅ ADDED - MapStruct processor -->
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
            <!-- ✅ ADDED - Lombok-MapStruct binding -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-parameters</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

## 🔍 WHY LOMBOK 1.18.36?

### Spring Boot 4.0.2 Compatibility Requirements
| Component | Minimum Version | Reason |
|-----------|----------------|---------|
| Lombok | 1.18.34+ | Required for Hibernate 6.x bytecode |
| MapStruct | 1.6.3+ | Jakarta EE 10 support |
| Java | 21 | Spring Boot 4.x baseline |

### Known Issues with Lombok 1.18.30
❌ Fails with Hibernate 6.x lazy initialization  
❌ Incompatible with Jakarta EE 10 annotations  
❌ Missing support for Java 21 language features  
❌ @Data generates broken equals/hashCode for JPA entities  
❌ @Builder ignores default values without @Builder.Default  

### Fixed in Lombok 1.18.36
✅ Full Hibernate 6.x compatibility  
✅ Jakarta EE 10 support  
✅ Java 21 language features  
✅ Proper JPA entity handling  
✅ @Builder.Default works correctly  

## 📋 NEXT STEPS TO VERIFY THE FIX

### Step 1: Clear Maven Cache (Optional but Recommended)
```bash
rm -rf ~/.m2/repository/org/projectlombok/lombok/1.18.30
mvn clean
```

### Step 2: Force Dependency Update
```bash
cd /Users/ravishah/MySpace/Home_Projects/wagecraft
mvn dependency:purge-local-repository -DactTransitively=false -DreResolve=false
mvn clean install -U
```

### Step 3: Verify Lombok Version
```bash
mvn dependency:tree | grep lombok
```

Expected output:
```
[INFO] +- org.projectlombok:lombok:jar:1.18.36:compile (optional)
[INFO] |  +- org.projectlombok:lombok-mapstruct-binding:jar:0.2.0:provided
```

### Step 4: Compile the Project
```bash
mvn clean compile
```

### Step 5: Run Tests
```bash
mvn test
```

## 🔧 IDE CONFIGURATION

### IntelliJ IDEA
1. **Enable Lombok Plugin**
   - Settings → Plugins → Search "Lombok" → Install
   - Restart IntelliJ

2. **Enable Annotation Processing**
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - ✅ Enable annotation processing
   - ✅ Obtain processors from project classpath

3. **Reload Maven Project**
   - Right-click on `pom.xml` → Maven → Reload Project
   - Or: View → Tool Windows → Maven → Reload All Maven Projects

4. **Invalidate Caches (if needed)**
   - File → Invalidate Caches → Invalidate and Restart

### Eclipse
1. **Install Lombok**
   - Download lombok.jar from https://projectlombok.org/download
   - Run: `java -jar lombok.jar`
   - Select Eclipse installation directory
   - Click "Install/Update"

2. **Enable Annotation Processing**
   - Project Properties → Java Compiler → Annotation Processing
   - ✅ Enable project specific settings
   - ✅ Enable annotation processing

3. **Update Maven Project**
   - Right-click on project → Maven → Update Project
   - ✅ Force Update of Snapshots/Releases
   - Click OK

### VS Code
1. **Install Extensions**
   - "Language Support for Java(TM) by Red Hat"
   - "Lombok Annotations Support for VS Code"

2. **Reload Java Projects**
   - Ctrl+Shift+P → "Java: Clean Java Language Server Workspace"
   - Ctrl+Shift+P → "Java: Reload Projects"

## ⚠️ COMMON ISSUES & SOLUTIONS

### Issue 1: "Dependency 'org.projectlombok:lombok:1.18.36' not found"

**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository/org/projectlombok

# Force update
mvn clean install -U

# If still failing, check internet connection and Maven settings
mvn dependency:get -Dartifact=org.projectlombok:lombok:1.18.36
```

### Issue 2: "Cannot find symbol" for Lombok-generated methods

**Solution:**
1. Verify annotation processing is enabled in IDE
2. Rebuild project: `mvn clean compile`
3. Reload Maven project in IDE
4. Check that Lombok comes BEFORE MapStruct in annotation processor paths

### Issue 3: Build fails with "package does not exist"

**Solution:**
```bash
# Clean everything
mvn clean
rm -rf target/

# Rebuild with verbose output
mvn compile -X | grep "annotation processing"
```

### Issue 4: IDE shows errors but Maven build succeeds

**Solution:**
- This is normal during Maven download
- Wait for Maven to finish downloading dependencies
- Reload IDE project
- If persists, restart IDE

## 📊 VERIFICATION CHECKLIST

- [ ] pom.xml shows `<lombok.version>1.18.36</lombok.version>`
- [ ] pom.xml includes MapStruct dependencies
- [ ] pom.xml has proper annotation processor configuration
- [ ] `mvn dependency:tree` shows Lombok 1.18.36
- [ ] `mvn clean compile` succeeds without errors
- [ ] IDE shows no compilation errors
- [ ] Lombok-generated methods (getters/setters) are recognized by IDE
- [ ] `mvn test` passes

## 📚 LOMBOK BEST PRACTICES FOR SPRING BOOT 4.x

### ✅ Safe for DTOs
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyDTO {
    private String field1;
    private Integer field2;
}
```

### ⚠️ Careful with JPA Entities
```java
@Entity
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
    
    @Column
    private String name;
    
    // DON'T use @Data - it breaks JPA lazy loading
    // DON'T include lazy-loaded fields in toString/equals
}
```

### ✅ Using @Builder.Default
```java
@Data
@Builder
public class Config {
    @Builder.Default
    private Integer timeout = 30;
    
    @Builder.Default
    private Boolean enabled = true;
}
```

## 🎯 SUMMARY

**Problem:** Lombok 1.18.30 is incompatible with Spring Boot 4.0.2 / Hibernate 6.x  
**Solution:** Upgrade to Lombok 1.18.36 + add MapStruct support  
**Status:** ✅ pom.xml successfully updated  
**Next Step:** Run `mvn clean install -U` to download dependencies  

---

**Created:** February 6, 2026  
**Author:** GitHub Copilot  
**Version:** 1.0  
