# Revision History

*This file was last reviewed on 2025-05-19*. **Please, keep this note updated**.

## 5.0-SNAPSHOT
* New ETL in rres-endpoint, which runs the new Nova Initialiser.

## 4.0.2 Last release before the new KnetMiner Nova Initialiser

*We should have issued a 5.0 release, but we didn't by mistake.*

* Documentation added, including walk-through doc.

### `rres-endpoints/`
* major revision and simplification of the ETL.
* Configs for new datasets added/updated:
  * cereals-premium
  * Brassica
  * Tropicana
  * Solanaceae
  * Plants Lite
  * Fungi Lite


## 4.0.1
* Old/outdated vavr library removed.
* Various dependencies upgraded. 

## 4.0
* Migration to Spring 6, directly and from other dependencies (Ondex, jutils). **WARNING: no backward compatibility guaranteed**.

## 3.0
* Migrated to Java 17. **WARNING: no backward compatibility guaranteed**.

## 2.2
* Documentation added (ETL diagrams).
* ETL scripts updated

## 2.1
* Documentation for raw data pipelines added.
* Minor options added to `CyQueriesReader`.

## 2.0
* First Java 11 version. **No former JDK versions supported since this**.
* new poaceae-based sample dataset.
* Migration to Neo4j 4.3.
* New RRes/Cyverse backend pipelines.

## 1.0
* First release.
