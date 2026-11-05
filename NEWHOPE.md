# Fork microrv32 dla newhope (FPGA-snake)

Fork https://github.com/agra-uni-bremen/microrv32 (MIT, (c) University of Bremen).
Gałąź robocza: `newhope`. Upstream: `git remote add upstream https://github.com/agra-uni-bremen/microrv32`.

## Zmiany względem upstreamu (`master`, ada1264)
| | upstream | newhope |
| --- | --- | --- |
| Scala / SpinalHDL / sbt | 2.12.18 / 1.10.2a / 1.3.3 | 2.13.14 / 1.12.3 / 1.12.4 (jak fpga-hw-lib) |
| id projektu sbt | `projectname` | `microrv32` |

- `SimpleBusSlaveFactory`: `ArrayBuffer` → `.toSeq` (Scala 2.13).
- `CSRUnit`: `allowUnsetRegToAvoidLatch` na stałych CSR (mvendorid, marchid, mimpid,
  mhartid, misa) — Spinal 1.12 zgłaszał UNASSIGNED REGISTER i pierwsza elaboracja padała.
- `test/RiscvTestsSuite.scala`: oficjalne `riscv-tests` z `sw/riscv-test-rv32imc-prebuilt`
  (rv32ui, rv32um, rv32uc — 48 testów) na `MicroRV32Top` rv32imc, Verilator:
  ```
  cd microrv32 && sbt test
  ```
Logika rdzenia bez zmian.

## Jako submodule
```
git submodule add -b newhope https://github.com/TomekBarabasz/microrv32 hw/cpu/microrv32
```
`hw/build.sbt` konsumenta (build sbt jest w podkatalogu `microrv32/`):
```scala
lazy val microrv32 = ProjectRef(file("cpu/microrv32/microrv32"), "microrv32")
```
Użycie: FPGA-snake `hw/demo/mimas_rv` (Mimas V2: monitor przez UART + LPDDR).
