# Backend-to-Frontend Field Mapping Reference

> Comprehensive mapping of every backend DTO field to its corresponding frontend TypeScript interface field.
> Covers all domains: Property, Rental Application, Lease, Payment, Employee, Auth, and Organization.

---

## Table of Contents

1. [Property Domain](#1-property-domain)
2. [Rental Application Domain](#2-rental-application-domain)
3. [Lease Domain](#3-lease-domain)
4. [Payment Domain](#4-payment-domain)
5. [Employee Domain](#5-employee-domain)
6. [Auth Domain](#6-auth-domain)
7. [Organization Domain](#7-organization-domain)
8. [Global Conventions](#8-global-conventions)
9. [Known Gaps & Mismatches](#9-known-gaps--mismatches)

---

## 1. Property Domain

### Response Structure

The backend returns a nested `PropertyResponse` with 13 section DTOs. The frontend `Property` interface is **flat** — all fields from every section are flattened into a single object.

**Backend:** `PropertyResponse` → `propertize/src/main/java/com/propertize/dto/property/response/PropertyResponse.java`
**Frontend:** `Property` → `propertize-front-end/src/types/property.types.ts`

| Backend Section DTO            | Frontend Mapping Strategy         |
| ------------------------------ | --------------------------------- |
| `PropertyBasicInfoDTO`         | Flattened into root `Property`    |
| `PropertyPhysicalDetailsDTO`   | Flattened into root `Property`    |
| `PropertyFinancialDTO`         | Flattened into root `Property`    |
| `PropertyAmenitiesDTO`         | Flattened into root `Property`    |
| `PropertyUtilitiesDTO`         | Flattened into root `Property`    |
| `PropertyLeaseRequirementsDTO` | Flattened into root `Property`    |
| `PropertyPetPolicyDTO`         | Flattened into root `Property`    |
| `PropertyMarketingDTO`         | Flattened into root `Property`    |
| `PropertyMaintenanceDTO`       | Flattened into root `Property`    |
| `PropertyLegalDTO`             | Flattened into root `Property`    |
| `PropertyOccupancyDTO`         | Flattened into root `Property`    |
| `PropertyExtendedDetailsDTO`   | Nested as `extendedDetails`       |
| `PropertyPerformanceDTO`       | Separate (not in base `Property`) |

### 1.1 PropertyBasicInfoDTO

| Backend Field (Java) | Type                 | Frontend Field (TS)    | Type                          | Status                            |
| -------------------- | -------------------- | ---------------------- | ----------------------------- | --------------------------------- |
| `propertyId`         | `String`             | `id`                   | `string`                      | ⚠️ Name diff                      |
| `propertyName`       | `String`             | `propertyName`         | `string`                      | ✅                                |
| `status`             | `PropertyStatusEnum` | `status`               | `PropertyStatusEnum` (string) | ⚠️ Enum→string                    |
| `type`               | `PropertyTypeEnum`   | `type`                 | `PropertyTypeEnum` (string)   | ⚠️ Enum→string                    |
| `address`            | `String`             | `address`              | `PropertyAddress`             | ⚠️ String vs object               |
| `city`               | `String`             | (in `address.city`)    | `string`                      | ⚠️ Nested                         |
| `state`              | `StateProvinceEnum`  | (in `address.state`)   | `string`                      | ⚠️ Enum→string                    |
| `zipCode`            | `String`             | (in `address.zip`)     | `string`                      | ⚠️ Name diff (`zipCode`→`zip`)    |
| `country`            | `String`             | (in `address.country`) | `string`                      | ⚠️ Nested                         |
| `bedrooms`           | `Integer`            | `bedrooms`             | `number`                      | ✅ Default: `0`                   |
| `bathrooms`          | `BigDecimal`         | `bathrooms`            | `number`                      | ✅ Default: `0`                   |
| `squareFeet`         | `BigDecimal`         | `squareFeet`           | `number`                      | ✅ Default: `0`                   |
| `monthlyRent`        | `BigDecimal`         | `monthlyRent`          | `number`                      | ✅ Default: `0`                   |
| `isActive`           | `Boolean`            | `isActive`             | `boolean`                     | ✅ Default: `false`               |
| `availableDate`      | `OffsetDateTime`     | `availableDate`        | `string` (ISO)                | ✅                                |
| `description`        | `String`             | `description`          | `string`                      | ✅                                |
| —                    | —                    | `propertyCode`         | `string`                      | ❌ Not in this DTO (from entity)  |
| —                    | —                    | `organizationId`       | `string`                      | ❌ From parent `PropertyResponse` |
| —                    | —                    | `furnishedStatus`      | `boolean`                     | ✅ Mapped from `listingInfo`      |

### 1.2 PropertyPhysicalDetailsDTO

| Backend Field (Java)     | Type                      | Frontend Field (TS)      | Type           | Status                                             |
| ------------------------ | ------------------------- | ------------------------ | -------------- | -------------------------------------------------- |
| `buildingName`           | `String`                  | `buildingName`           | `string`       | ✅                                                 |
| `floorNumber`            | `String`                  | `floorNumber`            | `string`       | ✅                                                 |
| `totalFloorsInBuilding`  | `Integer`                 | `totalFloorsInBuilding`  | `number`       | ✅                                                 |
| `totalUnitsInBuilding`   | `Integer`                 | `totalUnitsInBuilding`   | `number`       | ✅                                                 |
| `constructionType`       | `String` (enum `.name()`) | `constructionType`       | `string`       | ✅                                                 |
| `roofType`               | `String` (enum `.name()`) | `roofType`               | `string`       | ✅                                                 |
| `foundationType`         | `String` (enum `.name()`) | `foundationType`         | `string`       | ✅                                                 |
| `exteriorMaterial`       | `String`                  | `exteriorMaterial`       | `string`       | ✅                                                 |
| `flooringType`           | `String` (enum `.name()`) | `flooringType`           | `string`       | ✅                                                 |
| `renovationDetails`      | `String`                  | `renovationDetails`      | `string`       | ✅                                                 |
| `propertyCondition`      | `String` (enum `.name()`) | `propertyCondition`      | `string`       | ✅                                                 |
| `lastRenovationDate`     | `LocalDate`               | `lastRenovationDate`     | `string` (ISO) | ✅                                                 |
| `lastInspectionDate`     | `LocalDate`               | `lastInspectionDate`     | `string` (ISO) | ✅                                                 |
| `nextInspectionDate`     | `LocalDate`               | `nextInspectionDate`     | `string` (ISO) | ✅                                                 |
| `hasElevator`            | `Boolean`                 | `hasElevator`            | `boolean`      | ✅ Default: `false`                                |
| `isWheelchairAccessible` | `Boolean`                 | `isWheelchairAccessible` | `boolean`      | ✅ Default: `false`                                |
| `viewType`               | `String` (enum `.name()`) | `viewType`               | `string`       | ✅                                                 |
| `exposureDirection`      | `String` (enum `.name()`) | `exposureDirection`      | `string`       | ✅                                                 |
| `hasNaturalLight`        | `Boolean`                 | `hasNaturalLight`        | `boolean`      | ✅ Default: `false`                                |
| —                        | —                         | `unitNumber`             | `string`       | ❌ Frontend-only field                             |
| —                        | —                         | `yearBuilt`              | `number`       | ❌ Not in this DTO (from `buildingInfo.yearBuilt`) |

### 1.3 PropertyFinancialDTO

| Backend Field (Java)     | Type         | Frontend Field (TS)      | Type           | Status                    |
| ------------------------ | ------------ | ------------------------ | -------------- | ------------------------- |
| `monthlyRent`            | `BigDecimal` | `monthlyRent`            | `number`       | ✅ Default: `0`           |
| `securityDeposit`        | `BigDecimal` | `securityDeposit`        | `number`       | ✅ Default: `0`           |
| `applicationFee`         | `BigDecimal` | `applicationFee`         | `number`       | ✅ Default: `0`           |
| `lastMonthRent`          | `BigDecimal` | `lastMonthRent`          | `number`       | ✅                        |
| `keyDeposit`             | `BigDecimal` | `keyDeposit`             | `number`       | ✅                        |
| `cleaningFee`            | `BigDecimal` | `cleaningFee`            | `number`       | ✅                        |
| `landlordId`             | `String`     | `landlordId`             | `string`       | ✅                        |
| `ownerName`              | `String`     | `ownerName`              | `string`       | ✅                        |
| `ownerEmail`             | `String`     | `ownerEmail`             | `string`       | ✅                        |
| `ownerPhone`             | `String`     | `ownerPhone`             | `string`       | ✅                        |
| `purchaseDate`           | `LocalDate`  | `purchaseDate`           | `string` (ISO) | ✅                        |
| `purchasePrice`          | `BigDecimal` | `purchasePrice`          | `number`       | ✅                        |
| `currentMarketValue`     | `BigDecimal` | `currentMarketValue`     | `number`       | ✅                        |
| `lastAppraisalDate`      | `LocalDate`  | `lastAppraisalDate`      | `string` (ISO) | ✅                        |
| `annualPropertyTax`      | `BigDecimal` | `annualPropertyTax`      | `number`       | ✅                        |
| `parcelNumber`           | `String`     | `parcelNumber`           | `string`       | ✅ (also in Legal)        |
| `monthlyHoaFees`         | `BigDecimal` | `monthlyHoaFees`         | `number`       | ✅                        |
| `annualInsurancePremium` | `BigDecimal` | `annualInsurancePremium` | `number`       | ✅                        |
| —                        | —            | `propertyTaxId`          | `string`       | ❌ Not mapped from entity |
| —                        | —            | `hoaName`                | `string`       | ❌ Not mapped from entity |
| —                        | —            | `hoaEmail`               | `string`       | ❌ Not mapped from entity |
| —                        | —            | `hoaPhone`               | `string`       | ❌ Not mapped from entity |
| —                        | —            | `insuranceProvider`      | `string`       | ❌ Not mapped from entity |
| —                        | —            | `insurancePolicyNumber`  | `string`       | ❌ Not mapped from entity |
| —                        | —            | `insuranceExpiryDate`    | `string`       | ❌ Not mapped from entity |

### 1.4 PropertyAmenitiesDTO

| Backend Field (Java) | Type                      | Frontend Field (TS)     | Type       | Status              |
| -------------------- | ------------------------- | ----------------------- | ---------- | ------------------- |
| `amenities`          | `List<String>`            | `amenities`             | `string[]` | ✅ Default: `[]`    |
| `hasPool`            | `Boolean`                 | `hasPool`               | `boolean`  | ✅ Default: `false` |
| `hasGym`             | `Boolean`                 | `hasGym`                | `boolean`  | ✅ Default: `false` |
| `hasHeating`         | `Boolean`                 | `hasHeating`            | `boolean`  | ✅ Default: `false` |
| `hasAirConditioning` | `Boolean`                 | `hasAirConditioning`    | `boolean`  | ✅ Default: `false` |
| `hasLaundry`         | `Boolean`                 | `hasLaundry`            | `boolean`  | ✅ Default: `false` |
| `hasParking`         | `Boolean`                 | `hasParking`            | `boolean`  | ✅ Default: `false` |
| `hasFireplace`       | `Boolean`                 | `hasFireplace`          | `boolean`  | ✅ Default: `false` |
| `numberOfFireplaces` | `Integer`                 | `numberOfFireplaces`    | `number`   | ✅ Default: `0`     |
| `hasWasherDryer`     | `Boolean`                 | `hasWasherDryer`        | `boolean`  | ✅ Default: `false` |
| `washerDryerType`    | `String` (enum `.name()`) | `washerDryerType`       | `string`   | ✅                  |
| `hasDishwasher`      | `Boolean`                 | `hasDishwasher`         | `boolean`  | ✅ Default: `false` |
| `hasMicrowave`       | `Boolean`                 | `hasMicrowave`          | `boolean`  | ✅ Default: `false` |
| `hasGarbageDisposal` | `Boolean`                 | `hasGarbageDisposal`    | `boolean`  | ✅ Default: `false` |
| `hasCeilingFans`     | `Boolean`                 | `hasCeilingFans`        | `boolean`  | ✅ Default: `false` |
| `hasWalkInCloset`    | `Boolean`                 | `hasWalkInCloset`       | `boolean`  | ✅ Default: `false` |
| `hasPatio`           | `Boolean`                 | `hasPatio`              | `boolean`  | ✅ Default: `false` |
| `patioSize`          | `BigDecimal`              | `patioSize`             | `number`   | ✅                  |
| `hasYard`            | `Boolean`                 | `hasYard`               | `boolean`  | ✅ Default: `false` |
| `yardSize`           | `BigDecimal`              | `yardSize`              | `number`   | ✅                  |
| `isFenced`           | `Boolean`                 | `isFenced`              | `boolean`  | ✅ Default: `false` |
| `hasBalcony`         | `Boolean`                 | `hasBalcony`            | `boolean`  | ✅ Default: `false` |
| `hasStorage`         | `Boolean`                 | `hasStorage`            | `boolean`  | ✅ Default: `false` |
| `hasSecuritySystem`  | `Boolean`                 | `hasSecuritySystem`     | `boolean`  | ✅ Default: `false` |
| —                    | —                         | `assignedParkingSpaces` | `number`   | ❌ No entity field  |
| —                    | —                         | `parkingSpaces`         | `number`   | ❌ No entity field  |
| —                    | —                         | `parkingLocation`       | `string`   | ❌ No entity field  |
| —                    | —                         | `parkingSpaceNumbers`   | `string`   | ❌ No entity field  |
| —                    | —                         | `hasGatedAccess`        | `boolean`  | ❌ No entity field  |

### 1.5 PropertyUtilitiesDTO

| Backend Field (Java)      | Type      | Frontend Field (TS)       | Type      | Status                    |
| ------------------------- | --------- | ------------------------- | --------- | ------------------------- |
| `utilitiesIncluded`       | `Boolean` | `utilitiesIncluded`       | `boolean` | ✅ Default: `false`       |
| `includedUtilities`       | `String`  | `includedUtilities`       | `string`  | ❌ Not mapped from entity |
| `waterProvider`           | `String`  | `waterProvider`           | `string`  | ✅                        |
| `waterAccountNumber`      | `String`  | `waterAccountNumber`      | `string`  | ✅                        |
| `electricProvider`        | `String`  | `electricProvider`        | `string`  | ✅                        |
| `electricAccountNumber`   | `String`  | `electricAccountNumber`   | `string`  | ✅                        |
| `gasProvider`             | `String`  | `gasProvider`             | `string`  | ✅                        |
| `gasAccountNumber`        | `String`  | `gasAccountNumber`        | `string`  | ✅                        |
| `internetProvider`        | `String`  | `internetProvider`        | `string`  | ✅                        |
| `trashCollectionProvider` | `String`  | `trashCollectionProvider` | `string`  | ✅                        |
| `trashCollectionSchedule` | `String`  | `trashCollectionSchedule` | `string`  | ✅                        |

### 1.6 PropertyLeaseRequirementsDTO

| Backend Field (Java)                 | Type         | Frontend Field (TS)                  | Type      | Status              |
| ------------------------------------ | ------------ | ------------------------------------ | --------- | ------------------- |
| `minimumLeaseTerm`                   | `Integer`    | `minimumLeaseTerm`                   | `number`  | ✅                  |
| `maximumLeaseTerm`                   | `Integer`    | `maximumLeaseTerm`                   | `number`  | ✅                  |
| `maxOccupants`                       | `Integer`    | `maxOccupants`                       | `number`  | ✅                  |
| `minimumCreditScore`                 | `Integer`    | `minimumCreditScore`                 | `number`  | ✅                  |
| `minimumIncomeMultiplier`            | `BigDecimal` | `minimumIncomeMultiplier`            | `number`  | ✅                  |
| `backgroundCheckRequired`            | `Boolean`    | `backgroundCheckRequired`            | `boolean` | ✅ Default: `false` |
| `employmentVerificationRequired`     | `Boolean`    | `employmentVerificationRequired`     | `boolean` | ✅ Default: `false` |
| `previousLandlordReferencesRequired` | `Boolean`    | `previousLandlordReferencesRequired` | `boolean` | ✅ Default: `false` |
| `numberOfReferencesRequired`         | `Integer`    | `numberOfReferencesRequired`         | `number`  | ✅                  |
| `requiredInsuranceCoverage`          | `BigDecimal` | `requiredInsuranceCoverage`          | `number`  | ✅                  |
| `requiresRentersInsurance`           | `Boolean`    | `requiresRentersInsurance`           | `boolean` | ✅ Default: `false` |
| `allowsSubleasing`                   | `Boolean`    | `allowsSubleasing`                   | `boolean` | ✅ Default: `false` |
| `allowsShortTermRentals`             | `Boolean`    | `allowsShortTermRentals`             | `boolean` | ✅ Default: `false` |

### 1.7 PropertyPetPolicyDTO

| Backend Field (Java) | Type         | Frontend Field (TS) | Type      | Status              |
| -------------------- | ------------ | ------------------- | --------- | ------------------- |
| `isPetAllowed`       | `Boolean`    | `isPetAllowed`      | `boolean` | ✅ Default: `false` |
| `petDeposit`         | `BigDecimal` | `petDeposit`        | `number`  | ✅                  |
| `monthlyPetRent`     | `BigDecimal` | `monthlyPetRent`    | `number`  | ✅                  |
| `maxPetsAllowed`     | `Integer`    | `maxPetsAllowed`    | `number`  | ✅                  |
| `petRestrictions`    | `String`     | `petRestrictions`   | `string`  | ✅                  |
| `petWeightLimit`     | `BigDecimal` | `petWeightLimit`    | `number`  | ✅                  |

### 1.8 PropertyMarketingDTO

| Backend Field (Java) | Type          | Frontend Field (TS)    | Type       | Status        |
| -------------------- | ------------- | ---------------------- | ---------- | ------------- |
| `photoUrls`          | `List<Photo>` | `photoUrls`            | `string[]` | ⚠️ Type diff  |
| `virtualTourUrl`     | `String`      | `virtualTourUrl`       | `string`   | ✅            |
| `videoTourUrl`       | `String`      | `videoTourUrl`         | `string`   | ✅            |
| `floorPlanUrl`       | `String`      | `floorPlanUrl`         | `string`   | ✅            |
| —                    | —             | `marketingTitle`       | `string`   | ❌ Not mapped |
| —                    | —             | `marketingDescription` | `string`   | ❌ Not mapped |
| —                    | —             | `keySellingPoints`     | `string`   | ❌ Not mapped |
| —                    | —             | `isListedOnline`       | `boolean`  | ❌ Not mapped |
| —                    | —             | `isFeaturedProperty`   | `boolean`  | ❌ Not mapped |
| —                    | —             | `listingStartDate`     | `string`   | ❌ Not mapped |
| —                    | —             | `listingEndDate`       | `string`   | ❌ Not mapped |
| —                    | —             | `neighborhood`         | `string`   | ❌ Not mapped |
| —                    | —             | `elementarySchool`     | `string`   | ❌ Not mapped |
| —                    | —             | `middleSchool`         | `string`   | ❌ Not mapped |
| —                    | —             | `highSchool`           | `string`   | ❌ Not mapped |
| —                    | —             | `schoolDistrict`       | `string`   | ❌ Not mapped |
| —                    | —             | `walkScore`            | `number`   | ❌ Not mapped |

### 1.9 PropertyMaintenanceDTO

| Backend Field (Java)      | Type     | Frontend Field (TS)       | Type     | Status                                         |
| ------------------------- | -------- | ------------------------- | -------- | ---------------------------------------------- |
| `emergencyContactName`    | `String` | `emergencyContactName`    | `string` | ✅                                             |
| `emergencyContactPhone`   | `String` | `emergencyContactPhone`   | `string` | ✅                                             |
| `emergencyContactEmail`   | `String` | `emergencyContactEmail`   | `string` | ✅                                             |
| `maintenanceContactName`  | `String` | `maintenanceContactName`  | `string` | ✅                                             |
| `maintenanceContactPhone` | `String` | `maintenanceContactPhone` | `string` | ✅                                             |
| `maintenanceContactEmail` | `String` | `maintenanceContactEmail` | `string` | ✅                                             |
| `internalNotes`           | `String` | `internalNotes`           | `string` | ✅ Mapped from `notesInfo.landlordNotes`       |
| `specialInstructions`     | `String` | `specialInstructions`     | `string` | ✅ Mapped from `notesInfo.showingInstructions` |

### 1.10 PropertyLegalDTO

| Backend Field (Java)   | Type                      | Frontend Field (TS)    | Type     | Status             |
| ---------------------- | ------------------------- | ---------------------- | -------- | ------------------ |
| `propertyTitle`        | `String`                  | `propertyTitle`        | `string` | ✅                 |
| `legalLotNumber`       | `String`                  | `legalLotNumber`       | `string` | ✅                 |
| `subdivisionName`      | `String`                  | `subdivisionName`      | `string` | ✅                 |
| `legalDescription`     | `String`                  | `legalDescription`     | `string` | ✅                 |
| `zoningClassification` | `String` (enum `.name()`) | `zoningClassification` | `string` | ✅                 |
| —                      | —                         | `gateAccessCode`       | `string` | ❌ No entity field |
| —                      | —                         | `buildingAccessCode`   | `string` | ❌ No entity field |
| —                      | —                         | `mailboxLocation`      | `string` | ❌ No entity field |
| —                      | —                         | `mailboxNumber`        | `string` | ❌ No entity field |
| —                      | —                         | `termsAndConditions`   | `string` | ❌ No entity field |

### 1.11 PropertyOccupancyDTO

| Backend Field (Java) | Type        | Frontend Field (TS)   | Type           | Status              |
| -------------------- | ----------- | --------------------- | -------------- | ------------------- |
| `currentLeaseId`     | `String`    | `currentLeaseId`      | `string`       | ✅                  |
| `currentTenantId`    | `String`    | `currentTenantId`     | `string`       | ✅                  |
| `propertyManagerId`  | `String`    | `propertyManagerId`   | `string`       | ✅                  |
| `isArchived`         | `Boolean`   | `isArchived`          | `boolean`      | ✅ Default: `false` |
| `archivedDate`       | `LocalDate` | `archivedDate`        | `string` (ISO) | ✅                  |
| `archivedReason`     | `String`    | `archivedReason`      | `string`       | ✅                  |
| —                    | —           | `propertySource`      | `string`       | ❌ Not mapped       |
| —                    | —           | `externalReferenceId` | `string`       | ❌ Not mapped       |

### 1.12 PropertyPerformanceDTO

| Backend Field (Java)       | Type                          | Frontend Field (TS) | Type | Status            |
| -------------------------- | ----------------------------- | ------------------- | ---- | ----------------- |
| `propertyId`               | `String`                      | —                   | —    | Separate endpoint |
| `propertyName`             | `String`                      | —                   | —    | Separate endpoint |
| `propertyCode`             | `String`                      | —                   | —    |                   |
| `performanceTier`          | `PropertyPerformanceTierEnum` | —                   | —    |                   |
| `performanceScore`         | `Integer`                     | —                   | —    |                   |
| `lifetimeRevenue`          | `BigDecimal`                  | —                   | —    |                   |
| `lifetimeExpenses`         | `BigDecimal`                  | —                   | —    |                   |
| `netOperatingIncome`       | `BigDecimal`                  | —                   | —    |                   |
| `returnOnInvestment`       | `BigDecimal`                  | —                   | —    |                   |
| `capRate`                  | `BigDecimal`                  | —                   | —    |                   |
| `occupancyRate`            | `BigDecimal`                  | —                   | —    |                   |
| `totalVacantDays`          | `Integer`                     | —                   | —    |                   |
| `lastOccupiedDate`         | `LocalDate`                   | —                   | —    |                   |
| `totalLeasesCount`         | `Integer`                     | —                   | —    |                   |
| `leaseRenewalRate`         | `BigDecimal`                  | —                   | —    |                   |
| `totalTenantsCount`        | `Integer`                     | —                   | —    |                   |
| `avgTenantTenureMonths`    | `BigDecimal`                  | —                   | —    |                   |
| `totalMaintenanceRequests` | `Integer`                     | —                   | —    |                   |
| `avgMaintenanceDays`       | `BigDecimal`                  | —                   | —    |                   |
| `maintenanceCostPerMonth`  | `BigDecimal`                  | —                   | —    |                   |
| `firstListedDate`          | `LocalDate`                   | —                   | —    |                   |
| `lastRentIncreaseDate`     | `LocalDate`                   | —                   | —    |                   |
| `metricsCalculatedAt`      | `OffsetDateTime`              | —                   | —    |                   |

> **Note:** Performance metrics are typically fetched separately and not part of the base `Property` frontend type.

---

## 2. Rental Application Domain

**Backend:** `RentalApplicationResponse` → `propertize/src/main/java/com/propertize/dto/rental/response/RentalApplicationResponse.java`
**Frontend:** `RentalApplicationResponse` → `propertize-front-end/src/types/rental-application.types.ts`

### 2.1 RentalApplicationResponse (Top-Level)

| Backend Field (Java) | Type                           | Frontend Field (TS) | Type                     | Status       |
| -------------------- | ------------------------------ | ------------------- | ------------------------ | ------------ |
| `organizationId`     | `String`                       | `organizationId`    | `string`                 | ✅           |
| `propertyId`         | `String`                       | `propertyId`        | `string`                 | ✅           |
| `tenantId`           | `String`                       | `tenantId`          | `string`                 | ✅           |
| `creditScore`        | `Integer`                      | `creditScore`       | `number`                 | ✅           |
| `property`           | `PropertyBasicInfoDTO`         | `property`          | `PropertyBasicInfoDTO`   | ✅           |
| `workflow`           | `RentalApplicationWorkflowDTO` | `workflow`          | `ApplicationWorkflowDTO` | ⚠️ Name diff |
| `backgroundCheck`    | `BackgroundCheckInfoDTO`       | `backgroundCheck`   | `BackgroundCheckInfoDTO` | ✅           |
| `leaseDocument`      | `LeaseDocumentInfoDTO`         | `leaseDocument`     | `LeaseDocumentInfoDTO`   | ✅           |
| `applicant`          | `RentalApplicantInfoDTO`       | `applicant`         | `RentalApplicantInfoDTO` | ✅           |

### 2.2 RentalApplicationWorkflowDTO

| Backend Field (Java) | Type                          | Frontend Field (TS)  | Type                | Status            |
| -------------------- | ----------------------------- | -------------------- | ------------------- | ----------------- |
| `id`                 | `String`                      | `id`                 | `string`            | ✅                |
| `trackingId`         | `String`                      | `trackingId`         | `string`            | ✅                |
| `status`             | `RentalApplicationStatusEnum` | `status`             | `ApplicationStatus` | ⚠️ Type name diff |
| `submittedDate`      | `OffsetDateTime`              | `submittedDate`      | `string` (ISO)      | ✅                |
| `submittedBy`        | `String`                      | `submittedBy`        | `string`            | ✅                |
| `submittedByName`    | `String`                      | `submittedByName`    | `string`            | ✅                |
| `submittedNotes`     | `String`                      | `submittedNotes`     | `string`            | ✅                |
| `reviewedDate`       | `OffsetDateTime`              | `reviewedDate`       | `string` (ISO)      | ✅                |
| `reviewedBy`         | `String`                      | `reviewedBy`         | `string`            | ✅                |
| `reviewedByName`     | `String`                      | `reviewedByName`     | `string`            | ✅                |
| `reviewNotes`        | `String`                      | `reviewNotes`        | `string`            | ✅                |
| `notes`              | `String`                      | `notes`              | `string`            | ✅                |
| `approvedBy`         | `String`                      | `approvedBy`         | `string`            | ✅                |
| `approvedByName`     | `String`                      | `approvedByName`     | `string`            | ✅                |
| `approvedDate`       | `OffsetDateTime`              | `approvedDate`       | `string` (ISO)      | ✅                |
| `approvalNotes`      | `String`                      | `approvalNotes`      | `string`            | ✅                |
| `rejectedBy`         | `String`                      | `rejectedBy`         | `string`            | ✅                |
| `rejectedByName`     | `String`                      | `rejectedByName`     | `string`            | ✅                |
| `rejectedDate`       | `OffsetDateTime`              | `rejectedDate`       | `string` (ISO)      | ✅                |
| `rejectionReason`    | `String`                      | `rejectionReason`    | `string`            | ✅                |
| `cancelledDate`      | `OffsetDateTime`              | `cancelledDate`      | `string` (ISO)      | ✅                |
| `cancelledBy`        | `String`                      | `cancelledBy`        | `string`            | ✅                |
| `cancellationReason` | `String`                      | `cancellationReason` | `string`            | ✅                |
| `assignedTo`         | `String`                      | `assignedTo`         | `string`            | ✅                |
| `assignedToName`     | `String`                      | `assignedToName`     | `string`            | ✅                |
| `assignedBy`         | `String`                      | `assignedBy`         | `string`            | ✅                |
| `assignedDate`       | `OffsetDateTime`              | `assignedDate`       | `string` (ISO)      | ✅                |

### 2.3 RentalApplicantInfoDTO

| Backend Field (Java) | Type                    | Frontend Field (TS)         | Type                    | Status                      |
| -------------------- | ----------------------- | --------------------------- | ----------------------- | --------------------------- |
| `firstName`          | `String`                | `firstName`                 | `string`                | ✅                          |
| `lastName`           | `String`                | `lastName`                  | `string`                | ✅                          |
| `email`              | `String`                | `email`                     | `string`                | ✅                          |
| `phone`              | `String`                | `phone`                     | `string`                | ✅                          |
| `dateOfBirth`        | `LocalDate`             | `dateOfBirth`               | `string` (ISO)          | ✅                          |
| `currentAddress`     | `AddressDTO`            | `currentAddress`            | `AddressDTO`            | ✅                          |
| `employmentStatus`   | `EmploymentStatusEnum`  | `employmentStatus`          | `string`                | ⚠️ Enum→string              |
| `employmentInfo`     | `EmploymentInfoDTO`     | `employmentInfo`            | `EmploymentInfoDTO`     | ✅                          |
| `creditScore`        | `Integer`               | `creditScore`               | `number`                | ✅                          |
| `annualIncome`       | `BigDecimal`            | `annualIncome`              | `number`                | ✅                          |
| `bankruptcyHistory`  | `Boolean`               | `bankruptcyHistory`         | `boolean`               | ✅                          |
| `evictionHistory`    | `Boolean`               | `evictionHistory`           | `boolean`               | ✅                          |
| `desiredMoveInDate`  | `LocalDate`             | `desiredMoveInDate`         | `string` (ISO)          | ✅                          |
| `numberOfOccupants`  | `Integer`               | `numberOfOccupants`         | `number`                | ✅                          |
| `petInfo`            | `PetInfoDTO`            | `petInfo`                   | `PetInfoDTO`            | ✅                          |
| `emergencyContact`   | `ContactInfoDTO`        | `emergencyContact`          | `ContactInfoDTO`        | ✅                          |
| `previousResidence`  | `PreviousResidenceDTO`  | `previousResidence`         | `PreviousResidenceDTO`  | ✅                          |
| `coApplicantInfo`    | `CoApplicantInfoDTO`    | `coApplicantInfo`           | `CoApplicantInfoDTO`    | ✅                          |
| `vehicleInfo`        | `VehicleInfoDTO`        | `vehicleInfo`               | `VehicleInfoDTO`        | ✅                          |
| `personalReference1` | `ContactInfoDTO`        | (in `personalReferences[]`) | `ContactInfoDTO[]`      | ⚠️ Array vs separate fields |
| `personalReference2` | `ContactInfoDTO`        | (in `personalReferences[]`) | `ContactInfoDTO[]`      | ⚠️ Array vs separate fields |
| `submissionMetadata` | `SubmissionMetadataDTO` | `submissionMetadataInfo`    | `SubmissionMetadataDTO` | ⚠️ Name diff                |
| `capturedAt`         | `OffsetDateTime`        | —                           | —                       | ❌ Not in frontend          |

### 2.4 BackgroundCheckInfoDTO

| Backend Field (Java)   | Type                              | Frontend Field (TS)    | Type           | Status         |
| ---------------------- | --------------------------------- | ---------------------- | -------------- | -------------- |
| `required`             | `Boolean`                         | `required`             | `boolean`      | ✅             |
| `status`               | `BackgroundCheckStatusEnum`       | `status`               | `string`       | ⚠️ Enum→string |
| `passed`               | `Boolean`                         | `passed`               | `boolean`      | ✅             |
| `provider`             | `ScreeningProviderEnum`           | `provider`             | `string`       | ⚠️ Enum→string |
| `screeningId`          | `String`                          | `screeningId`          | `string`       | ✅             |
| `reportUrl`            | `String`                          | `reportUrl`            | `string`       | ✅             |
| `completedAt`          | `OffsetDateTime`                  | `completedAt`          | `string` (ISO) | ✅             |
| `comments`             | `String`                          | `comments`             | `string`       | ✅             |
| `waivedBy`             | `String`                          | `waivedBy`             | `string`       | ✅             |
| `waivedByName`         | `String`                          | `waivedByName`         | `string`       | ✅             |
| `waivedAt`             | `OffsetDateTime`                  | `waivedAt`             | `string` (ISO) | ✅             |
| `waiverReason`         | `BackgroundCheckWaiverReasonEnum` | `waiverReason`         | `string`       | ⚠️ Enum→string |
| `criminalRecordFound`  | `Boolean`                         | `criminalRecordFound`  | `boolean`      | ✅             |
| `evictionRecordFound`  | `Boolean`                         | `evictionRecordFound`  | `boolean`      | ✅             |
| `initiatedAt`          | `OffsetDateTime`                  | `initiatedAt`          | `string` (ISO) | ✅             |
| `initiatedBy`          | `String`                          | `initiatedBy`          | `string`       | ✅             |
| `referenceCheckStatus` | `ReferenceCheckStatusEnum`        | `referenceCheckStatus` | `string`       | ⚠️ Enum→string |

---

## 3. Lease Domain

**Backend:** `LeaseResponse` → `propertize/src/main/java/com/propertize/dto/lease/response/LeaseResponse.java`
**Frontend:** ❌ **No dedicated Lease types exist yet** — this is a known gap.

### 3.1 LeaseResponse (Top-Level)

| Backend Field (Java) | Type                        | Notes                     |
| -------------------- | --------------------------- | ------------------------- |
| `organizationId`     | `String`                    |                           |
| `organizationName`   | `String`                    |                           |
| `documentUrl`        | `String`                    |                           |
| `documentStatus`     | `String`                    |                           |
| `notes`              | `String`                    |                           |
| `internalNotes`      | `String`                    |                           |
| `version`            | `Long`                      |                           |
| `deleted`            | `Boolean`                   |                           |
| `basicInfo`          | `LeaseBasicInfoDTO`         | Contains id, status, type |
| `financial`          | `LeaseFinancialDTO`         | Rent, deposits, fees      |
| `tenant`             | `LeaseTenantDTO`            | Tenant details            |
| `property`           | `LeasePropertyDTO`          | Property summary          |
| `dates`              | `LeaseDatesDTO`             | All lease dates           |
| `terms`              | `LeaseTermsDTO`             | Terms, policies, rules    |
| `utilities`          | `LeaseUtilitiesDTO`         | Utility responsibility    |
| `audit`              | `LeaseAuditDTO`             | Created/updated tracking  |
| `performance`        | `LeasePerformanceDTO`       | Payment performance       |
| `complianceStatus`   | `LeaseComplianceStatusEnum` |                           |
| `totalRentCollected` | `BigDecimal`                |                           |
| `outstandingBalance` | `BigDecimal`                |                           |
| `onTimePaymentCount` | `Integer`                   |                           |
| `latePaymentCount`   | `Integer`                   |                           |

### 3.2 LeaseBasicInfoDTO

| Backend Field    | Type              |
| ---------------- | ----------------- |
| `id`             | `String`          |
| `leaseNumber`    | `String`          |
| `leaseTitle`     | `String`          |
| `status`         | `LeaseStatusEnum` |
| `leaseType`      | `LeaseTypeEnum`   |
| `organizationId` | `String`          |
| `active`         | `Boolean`         |
| `renewable`      | `Boolean`         |

### 3.3 LeaseFinancialDTO

| Backend Field       | Type         |
| ------------------- | ------------ |
| `monthlyRent`       | `BigDecimal` |
| `securityDeposit`   | `BigDecimal` |
| `petDeposit`        | `BigDecimal` |
| `keyDeposit`        | `BigDecimal` |
| `parkingFee`        | `BigDecimal` |
| `storageFee`        | `BigDecimal` |
| `lateFee`           | `BigDecimal` |
| `applicationFee`    | `BigDecimal` |
| `processingFee`     | `BigDecimal` |
| `totalMoveInCost`   | `BigDecimal` |
| `gracePeriodDays`   | `Integer`    |
| `rentDueDay`        | `Integer`    |
| `utilitiesIncluded` | `Boolean`    |

### 3.4 LeaseTenantDTO

| Backend Field       | Type      |
| ------------------- | --------- |
| `tenantId`          | `String`  |
| `tenantName`        | `String`  |
| `tenantEmail`       | `String`  |
| `tenantPhone`       | `String`  |
| `primaryTenant`     | `Boolean` |
| `numberOfOccupants` | `Integer` |
| `numberOfCoTenants` | `Integer` |
| `coTenantNames`     | `String`  |

### 3.5 LeaseDatesDTO

| Backend Field            | Type        |
| ------------------------ | ----------- |
| `startDate`              | `LocalDate` |
| `endDate`                | `LocalDate` |
| `moveInDate`             | `LocalDate` |
| `moveOutDate`            | `LocalDate` |
| `signedDate`             | `LocalDate` |
| `activationDate`         | `LocalDate` |
| `terminationDate`        | `LocalDate` |
| `renewalDeadline`        | `LocalDate` |
| `noticeToVacateDeadline` | `LocalDate` |
| `inspectionDate`         | `LocalDate` |
| `leaseDurationMonths`    | `Integer`   |
| `noticePeriodDays`       | `Integer`   |
| `renewable`              | `Boolean`   |
| `autoRenewal`            | `Boolean`   |

### 3.6 LeasePropertyDTO

| Backend Field       | Type         |
| ------------------- | ------------ |
| `propertyId`        | `String`     |
| `propertyName`      | `String`     |
| `propertyType`      | `String`     |
| `bedrooms`          | `Integer`    |
| `bathrooms`         | `Double`     |
| `squareFeet`        | `Integer`    |
| `parkingIncluded`   | `Boolean`    |
| `parkingSpotNumber` | `String`     |
| `storageIncluded`   | `Boolean`    |
| `storageUnitNumber` | `String`     |
| `propertyAddress`   | `AddressDTO` |

### 3.7 LeaseTermsDTO

| Backend Field                 | Type           |
| ----------------------------- | -------------- |
| `termsAndConditions`          | `String`       |
| `specialClauses`              | `String`       |
| `houseRules`                  | `String`       |
| `petPolicy`                   | `String`       |
| `petsAllowed`                 | `Boolean`      |
| `maxPets`                     | `Integer`      |
| `petRestrictions`             | `String`       |
| `smokingPolicy`               | `String`       |
| `smokingAllowed`              | `Boolean`      |
| `sublettingPolicy`            | `String`       |
| `sublettingAllowed`           | `Boolean`      |
| `guestPolicy`                 | `String`       |
| `maxGuestStayDays`            | `Integer`      |
| `maintenanceResponsibilities` | `String`       |
| `tenantResponsibilities`      | `String`       |
| `landlordResponsibilities`    | `String`       |
| `utilitiesIncluded`           | `List<String>` |
| `parkingRules`                | `String`       |
| `noisePolicy`                 | `String`       |
| `earlyTerminationPolicy`      | `String`       |
| `earlyTerminationAllowed`     | `Boolean`      |
| `earlyTerminationFeeMonths`   | `Integer`      |
| `renewalTerms`                | `String`       |
| `insuranceRequirements`       | `String`       |

---

## 4. Payment Domain

**Backend:** `payment-service/src/main/java/com/propertize/payment/dto/`
**Frontend:** `propertize-front-end/src/types/payment.types.ts`

### 4.1 PaymentResponse

| Backend Field (Java)    | Type                  | Frontend Field (TS)     | Type                  | Status                             |
| ----------------------- | --------------------- | ----------------------- | --------------------- | ---------------------------------- |
| `id`                    | `Long`                | `id`                    | `string`              | ⚠️ Long→string                     |
| `organizationId`        | `String`              | `organizationId`        | `string`              | ✅                                 |
| `tenantId`              | `String`              | `tenantId`              | `string`              | ✅                                 |
| `tenantName`            | `String`              | —                       | —                     | ❌ Frontend uses nested `tenant`   |
| `leaseId`               | `String`              | `leaseId`               | `string`              | ✅                                 |
| `propertyId`            | `String`              | `propertyId`            | `string`              | ✅                                 |
| `propertyAddress`       | `String`              | —                       | —                     | ❌ Frontend uses nested `property` |
| `vendorId`              | `String`              | —                       | —                     | ❌ Not in frontend                 |
| `ownerId`               | `String`              | —                       | —                     | ❌ Not in frontend                 |
| `amount`                | `BigDecimal`          | `amount`                | `number`              | ✅                                 |
| `lateFee`               | `BigDecimal`          | —                       | —                     | ❌ Not in frontend Payment         |
| `discount`              | `BigDecimal`          | —                       | —                     | ❌ Not in frontend Payment         |
| `netAmount`             | `BigDecimal`          | —                       | —                     | ❌ Not in frontend Payment         |
| `paymentDate`           | `LocalDate`           | `paymentDate`           | `string` (ISO)        | ✅                                 |
| `dueDate`               | `LocalDate`           | `dueDate`               | `string` (ISO)        | ✅                                 |
| `status`                | `PaymentStatusEnum`   | `status`                | `PaymentStatusEnum`   | ✅                                 |
| `paymentMethod`         | `PaymentMethodEnum`   | `paymentMethod`         | `PaymentMethodEnum`   | ✅                                 |
| `paymentCategory`       | `PaymentCategoryEnum` | `paymentCategory`       | `PaymentCategoryEnum` | ✅                                 |
| `paymentContext`        | `PaymentContextEnum`  | `paymentContext`        | `PaymentContextEnum`  | ✅                                 |
| `paymentType`           | `PaymentTypeEnum`     | `paymentType`           | `PaymentTypeEnum`     | ✅                                 |
| `paymentGateway`        | `PaymentGatewayEnum`  | `gateway`               | `PaymentGatewayEnum`  | ⚠️ Name diff                       |
| `stripePaymentIntentId` | `String`              | `stripePaymentIntentId` | `string`              | ✅                                 |
| `stripeChargeId`        | `String`              | —                       | —                     | ❌ Not in frontend                 |
| `transactionId`         | `String`              | `transactionId`         | `string`              | ✅                                 |
| `receiptUrl`            | `String`              | —                       | —                     | ❌ Not in frontend                 |
| `notes`                 | `String`              | `notes`                 | `string`              | ✅                                 |
| `failureReason`         | `String`              | `failureReason`         | `string`              | ✅                                 |
| `createdAt`             | `LocalDateTime`       | `createdAt`             | `string` (ISO)        | ✅                                 |
| `updatedAt`             | `LocalDateTime`       | `updatedAt`             | `string` (ISO)        | ✅                                 |
| `createdBy`             | `String`              | `createdByUserId`       | `string`              | ⚠️ Name diff                       |
| —                       | —                     | `unitId`                | `string`              | ❌ Backend doesn't have            |
| —                       | —                     | `currency`              | `string`              | ❌ Backend doesn't return          |
| —                       | —                     | `referenceNumber`       | `string`              | ❌ Only in Transaction             |
| —                       | —                     | `tenant`                | `PaymentTenant`       | ❌ Frontend nested display         |
| —                       | —                     | `property`              | `PaymentProperty`     | ❌ Frontend nested display         |

### 4.2 ApplicationFeeResponse

| Backend Field (Java)    | Type                | Frontend Field (TS) | Type | Status                  |
| ----------------------- | ------------------- | ------------------- | ---- | ----------------------- |
| `id`                    | `Long`              | —                   | —    | ⚠️ No dedicated FE type |
| `organizationId`        | `String`            | —                   | —    |                         |
| `rentalApplicationId`   | `String`            | —                   | —    |                         |
| `applicantId`           | `String`            | —                   | —    |                         |
| `applicantEmail`        | `String`            | —                   | —    |                         |
| `feeAmount`             | `BigDecimal`        | —                   | —    |                         |
| `discountAmount`        | `BigDecimal`        | —                   | —    |                         |
| `finalAmount`           | `BigDecimal`        | —                   | —    |                         |
| `promoCodeUsed`         | `String`            | —                   | —    |                         |
| `paymentStatus`         | `PaymentStatusEnum` | —                   | —    |                         |
| `stripePaymentIntentId` | `String`            | —                   | —    |                         |
| `stripeChargeId`        | `String`            | —                   | —    |                         |
| `dueDate`               | `LocalDate`         | —                   | —    |                         |
| `paidAt`                | `LocalDateTime`     | —                   | —    |                         |
| `createdAt`             | `LocalDateTime`     | —                   | —    |                         |

### 4.3 PromoCodeResponse

| Backend Field (Java) | Type                | Frontend Field (TS) | Type | Status                  |
| -------------------- | ------------------- | ------------------- | ---- | ----------------------- |
| `id`                 | `Long`              | —                   | —    | ⚠️ No dedicated FE type |
| `code`               | `String`            | —                   | —    |                         |
| `description`        | `String`            | —                   | —    |                         |
| `organizationId`     | `String`            | —                   | —    |                         |
| `discountType`       | `DiscountTypeEnum`  | —                   | —    |                         |
| `promoCodeType`      | `PromoCodeTypeEnum` | —                   | —    | **NEW** (just added)    |
| `discountValue`      | `BigDecimal`        | —                   | —    |                         |
| `maxUses`            | `Integer`           | —                   | —    |                         |
| `currentUses`        | `Integer`           | —                   | —    |                         |
| `expiresAt`          | `LocalDateTime`     | —                   | —    |                         |
| `active`             | `boolean`           | —                   | —    |                         |
| `expired`            | `boolean`           | —                   | —    |                         |
| `depleted`           | `boolean`           | —                   | —    |                         |
| `createdAt`          | `LocalDateTime`     | —                   | —    |                         |
| `createdBy`          | `String`            | —                   | —    |                         |

---

## 5. Employee Domain

**Backend:** `employee-service/src/main/java/com/propertize/platform/employecraft/dto/`
**Frontend:** ❌ **No dedicated Employee types exist yet** — this is a known gap.

### 5.1 Backend DTOs

| DTO Class             | Fields                                               | Types                                          |
| --------------------- | ---------------------------------------------------- | ---------------------------------------------- |
| `CompensationSummary` | `payType`, `payRate`, `payFrequency`, `annualSalary` | `String`, `BigDecimal`, `String`, `BigDecimal` |
| `DepartmentSummary`   | `id`, `name`, `code`                                 | `UUID`, `String`, `String`                     |
| `ManagerSummary`      | `id`, `fullName`, `email`                            | `UUID`, `String`, `String`                     |
| `PositionSummary`     | `id`, `title`, `code`                                | `UUID`, `String`, `String`                     |

---

## 6. Auth Domain

**Backend:** `auth-service/src/main/java/com/propertize/platform/auth/dto/`
**Frontend:** `propertize-front-end/src/types/api-responses.types.ts`

### 6.1 AuthResponse → LoginApiResponse

| Backend Field (Java) | Type                   | Frontend Field (TS) | Type            | Status              |
| -------------------- | ---------------------- | ------------------- | --------------- | ------------------- |
| `accessToken`        | `String`               | `accessToken`       | `string`        | ✅                  |
| `refreshToken`       | `String`               | `refreshToken`      | `string`        | ✅                  |
| `tokenType`          | `String`               | —                   | —               | ❌ Not in frontend  |
| `expiresIn`          | `Long`                 | `expiresIn`         | `number`        | ✅                  |
| `username`           | `String`               | `user.username`     | `string`        | ⚠️ Nested in `user` |
| `firstName`          | `String`               | `user.firstName`    | `string`        | ⚠️ Nested in `user` |
| `lastName`           | `String`               | `user.lastName`     | `string`        | ⚠️ Nested in `user` |
| `roles`              | `Set<String>`          | `user.roles`        | `string[]`      | ⚠️ Nested in `user` |
| `sessionId`          | `String`               | `sessionId`         | `string`        | ✅                  |
| `userDetails`        | `UserDetails` (nested) | `user`              | (nested object) | ⚠️ Name diff        |

### 6.2 UserDetails → LoginApiResponse.user

| Backend Field (Java) | Type          | Frontend Field (TS) | Type       | Status           |
| -------------------- | ------------- | ------------------- | ---------- | ---------------- |
| `id`                 | `Long`        | `id`                | `string`   | ⚠️ Long→string   |
| `username`           | `String`      | `username`          | `string`   | ✅               |
| `email`              | `String`      | `email`             | `string`   | ✅               |
| `firstName`          | `String`      | `firstName`         | `string`   | ✅               |
| `lastName`           | `String`      | `lastName`          | `string`   | ✅               |
| `organizationId`     | `String`      | `organizationId`    | `string`   | ✅               |
| `roles`              | `Set<String>` | `roles`             | `string[]` | ✅               |
| —                    | —             | `authorities`       | `string[]` | ❌ Frontend-only |

### 6.3 SessionUser (Frontend-only, built from AuthResponse)

| Frontend Field     | Type       | Source                            |
| ------------------ | ---------- | --------------------------------- |
| `id`               | `string`   | From `userDetails.id`             |
| `userId`           | `string`   | Same as `id`                      |
| `name`             | `string`   | Computed: `firstName + lastName`  |
| `email`            | `string`   | From `userDetails.email`          |
| `role`             | `string`   | Primary role                      |
| `roles`            | `string[]` | From `AuthResponse.roles`         |
| `permissions`      | `string[]` | Loaded separately via RBAC API    |
| `highestPrivilege` | `string`   | Computed from role hierarchy      |
| `organizationId`   | `string`   | From `userDetails.organizationId` |
| `accessToken`      | `string`   | From `AuthResponse.accessToken`   |
| `refreshToken`     | `string`   | From `AuthResponse.refreshToken`  |
| `sessionId`        | `string`   | From `AuthResponse.sessionId`     |

---

## 7. Organization Domain

**Frontend:** `propertize-front-end/src/types/organization.types.ts`
**Backend:** Organization entity in `auth-service`

| Frontend Field          | Type                 | Notes                        |
| ----------------------- | -------------------- | ---------------------------- |
| `id`                    | `string`             |                              |
| `organizationCode`      | `string`             |                              |
| `organizationName`      | `string`             |                              |
| `name`                  | `string`             | Alias for `organizationName` |
| `organizationType`      | `OrganizationType`   |                              |
| `slug`                  | `string`             |                              |
| `subscriptionTier`      | `SubscriptionTier`   |                              |
| `status`                | `SubscriptionStatus` |                              |
| `isInTrial`             | `boolean`            |                              |
| `trialEndsAt`           | `string`             |                              |
| `subscriptionStartDate` | `string`             |                              |
| `subscriptionExpiresAt` | `string`             |                              |
| `monthlyFee`            | `number`             |                              |
| `maxProperties`         | `number`             |                              |
| `maxUsers`              | `number`             |                              |
| `maxStorageGb`          | `number`             |                              |
| `propertyCount`         | `number`             |                              |
| `userCount`             | `number`             |                              |
| `storageUsedGb`         | `number`             |                              |

---

## 8. Global Conventions

### Type Serialization Rules

| Java Type             | JSON Serialization       | TypeScript Type             |
| --------------------- | ------------------------ | --------------------------- |
| `String`              | `"value"`                | `string`                    |
| `Integer` / `int`     | `123`                    | `number`                    |
| `Long`                | `123`                    | `number` or `string`        |
| `BigDecimal`          | `123.45`                 | `number`                    |
| `Boolean` / `boolean` | `true` / `false`         | `boolean`                   |
| `UUID`                | `"uuid-string"`          | `string`                    |
| `LocalDate`           | `"2025-01-15"`           | `string` (ISO 8601)         |
| `LocalDateTime`       | `"2025-01-15T10:30:00"`  | `string` (ISO 8601)         |
| `OffsetDateTime`      | `"2025-01-15T10:30:00Z"` | `string` (ISO 8601)         |
| `Enum`                | `"ENUM_VALUE"`           | `string` or TypeScript enum |
| `List<T>`             | `[...]`                  | `T[]`                       |
| `Set<T>`              | `[...]`                  | `T[]`                       |
| `Map<K,V>`            | `{...}`                  | `Record<K,V>`               |

### API Response Wrapper

All backend responses use `ApiResponse<T>`:

```json
{
  "status": "SUCCESS",
  "message": "Operation completed",
  "data": { ... },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

Frontend `httpClient` automatically unwraps `.data` from the response.

### Null Handling

- **Backend:** `PropertyDtoMapper` returns safe defaults for booleans (`false`), numeric fields (`0` / `BigDecimal.ZERO`), and collections (`Collections.emptyList()`)
- **Frontend:** Should still handle `null` / `undefined` defensively for optional fields that don't have backend defaults

---

## 9. Known Gaps & Mismatches

### Critical Gaps (No Frontend Types)

| Domain             | Backend DTO                                      | Frontend Type | Action Needed                       |
| ------------------ | ------------------------------------------------ | ------------- | ----------------------------------- |
| **Lease**          | `LeaseResponse` (9 nested DTOs)                  | ❌ Missing    | Create `lease.types.ts`             |
| **Employee**       | `CompensationSummary`, `DepartmentSummary`, etc. | ❌ Missing    | Create `employee.types.ts`          |
| **ApplicationFee** | `ApplicationFeeResponse`                         | ❌ Missing    | Create interface or reuse `Payment` |
| **PromoCode**      | `PromoCodeResponse`                              | ❌ Missing    | Create `promo-code.types.ts`        |

### Field Name Mismatches

| Backend                     | Frontend                     | Domain     |
| --------------------------- | ---------------------------- | ---------- |
| `propertyId` (in basicInfo) | `id`                         | Property   |
| `zipCode`                   | `zip` (in `PropertyAddress`) | Property   |
| `paymentGateway`            | `gateway`                    | Payment    |
| `createdBy`                 | `createdByUserId`            | Payment    |
| `userDetails`               | `user`                       | Auth       |
| `personalReference1/2`      | `personalReferences[]`       | Rental App |
| `submissionMetadata`        | `submissionMetadataInfo`     | Rental App |

### Type Mismatches

| Backend Type                 | Frontend Type        | Fields Affected                      |
| ---------------------------- | -------------------- | ------------------------------------ |
| `Enum` (Java)                | `string` (TS)        | All enum fields (status, type, etc.) |
| `Long` (Java)                | `string` (TS)        | `id` in Payment, Auth                |
| `BigDecimal`                 | `number`             | All monetary/decimal fields          |
| `LocalDate`/`OffsetDateTime` | `string`             | All date/time fields                 |
| `Boolean` (nullable)         | `boolean` (non-null) | All boolean property fields          |

### Structural Mismatches

| Issue                            | Details                                                                                    |
| -------------------------------- | ------------------------------------------------------------------------------------------ |
| **Nested vs Flat**               | Backend `PropertyResponse` is nested (13 sections); frontend `Property` is flat            |
| **Frontend-only nested objects** | Frontend `Payment` has `tenant`, `property`, `unit` nested objects not in backend DTO      |
| **Frontend computed fields**     | `SessionUser.name`, `SessionUser.highestPrivilege` are computed on the frontend            |
| **Array vs separate fields**     | Backend has `personalReference1`/`personalReference2`; frontend has `personalReferences[]` |

---

_Generated: April 2026 — Propertize Platform Team_
