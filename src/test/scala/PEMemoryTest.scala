package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


/*
interface of PE_memory:

    val rs1 = Input(Bits(width.W))
    val rs2 = Input(Bits(width.W))
    val is_load = Input(Bool())
    val is_store = Input(Bool())
    val result = Output(Bits(width.W))

    load: result := mem(rs1)
    store: mem(rs2) := rs1

*/

class PELoadStoreTest(m: PE_memory) extends PeekPokeTester(m){

    // store
    poke(m.io.rs1, 30.U)
    poke(m.io.rs2, 15.U)
    poke(m.io.is_load, false.B )
    poke(m.io.is_store, true.B )

    step(1)

    expect(m.io.result, 100.U )

    // load
    poke(m.io.rs1, 15.U)
    poke(m.io.rs2, 15.U)
    poke(m.io.is_load, true.B )
    poke(m.io.is_store, false.B )

    step(1)

    expect(m.io.result, 30.U )
}


class PEMemoryTests extends ChiselFlatSpec {

  val testerArgs = Array("")

  behavior of "MemoryTest"
  it should "load and store values correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE_memory()) {
      c => new PELoadStoreTest(c)
    } should be (true)
  }
  
}