[[_TOC_]]

# Null dereference analysis

## Pre-requisites

To run,

-   Java 8 SDK
-   Make build tool (Available in linux)

## Phase 1 (Intraprocedural analysis)

### Build

Make sure the pre-requisites are available on your system.
Run `make` from the main directory of this repository.

Please see `README.org` for more details on build.

### Usage

#### Direct

The analysis can be run directly from the command line, provided the java runtime is available on the system.

First add the soot packages to the class path using the command
`source environ.sh`

then use the following command to run the analysis. Here

-   `<target-folder>` is the directory where the java source file on which analysis is to be done resides.
-   `<main-class>` is the name of the class file within the target-folder
-   `<target-class>` is the class in the main-class file
-   `<target-method>` is the method on which the intra-procedural analysis will be done
    `java Analysis.java <target-folder> <main-class> <target-class> <target-method>`

#### Running tests

The `expected-output` directory has some of the tests that were manually verified. In order to run all the tests execute

`./run-all-tests.sh`
This executes all the tests and compares the output file with the one in expected-output folder.

You can run a specific test on one function by executing

`./run-one-test.sh <function-name>`

the scripts assume that the function is available in `target2-mine/BasicTest.java`

### Code structure

```mermaid
classDiagram
    interface LatticeElement
    LatticeElement: +LatticeElement tf_assign(LatticeElement l)
    LatticeElement: +LatticeElement tf_conditional(LatticeElement l)
    LatticeElement: +LatticeElement join_op(LatticeElement l)
    LatticeElement: +boolean equals(LatticeElement l)
    LatticeElement <|-- PointerLatticeElement

    class IPreProcess{
        ProgramPoint[] PreProcess(Soot.body body)
    }

    class PointerLatticePreProcess{

    }

    IPreProcess <|-- PointerLatticePreProcess

    class ProgramPoint{
        + LatticeElement latticeElement
        + Soot.stmt statement
        + boolean markedForPropagation
        + ProgramPoint[] successors
    }

    class Kildall{
        + LatticeElement[][] computeLFP(ProgramPoint[] points)
    }
```

#### IPreProcess

-   This interface is for preprocessors for particular lattice elements. It can be implemented based on the type of lattice being used for the analysis.

#### PointerLatticePreProcess

-   This is the implementation of IPreProcess for pointer lattice. Here we gather all the local variables and create an empty map, and also for each statement, the input facts are structured using the ProgramPoint class.

#### ProgramPoint

-   This class is to represent the program point, in Kildall's algorithm. Each program point has a statement, input fact, a flag to check whether its marked for propagation and the successor program points (which is obtained from the graph representation of the body).

#### Kildall

-   This class has just one static function to compute the least fixpoint. Given an input list of program points(This can be any latticeElement, which is instantiated based on the analysis type in main), it return list of list of program points.
-   The first element is always the final list of program points of the algorithm (The actual fixpoint).
-   The remaining elements are output at each iteration of the algorithm.

```mermaid
sequenceDiagram
    main->>PointerLatticePreProcess: Send soot body
    PointerLatticePreProcess->>main: return List<ProgramPoint>
    main->>Kildall: Send preprocessed List<ProgramPoint>
    Kildall->>main: return computed least-fixpoint as List<List<ProgramPoint>>
```
