package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class CustomInterfacePETests extends Module{

    val io = IO(new Bundle{

        val cmd_funct = Input(Bits(7.W))
        val cmd_rd = Input(Bits(5.W))
        val cmd_opcode = Input(Bits(7.W))
        val cmd_rs1 = Input(Bits(32.W))
        val cmd_rs2 = Input(Bits(32.W))
        val resp_data = Output(Bits(32.W))
        val done = Output(Bool())
        val ready = Input(Bool())
    })

    val PE = Module(new PE(1))

    // conecting inputs to module
    PE.io.cmd.funct := io.cmd_funct
    //PE.io.cmd.rd := io.cmd_rd
    PE.io.cmd.opcode := io.cmd_opcode
    PE.io.cmd.rs1 := io.cmd_rs1
    PE.io.cmd.rs2 := io.cmd_rs2
    PE.io.ready := io.ready

    // connecting outputs to module
    io.done := PE.io.done
    io.resp_data := PE.io.resp.data
    
    

}

/*
    this test aims at verifying if the state of the PE is "idle" when
    no input signal is given, and it swithces to state "exec" once
    io.ready is set to 1
*/
class StateMachineTest(m: CustomInterfacePETests) extends PeekPokeTester(m){

    // idle -> idle
    poke(m.io.ready, false.B)

    step(1) // 1 -> 2

    expect(m.io.done, false.B)
    // idle -> exec
    poke(m.io.ready, true.B)

    step(1) // 2 -> 3

    expect(m.io.done, true.B) 
    // here ideally I could test exec -> exec, but still the FSA always goes from exec -> idle
    poke(m.io.ready, false.B) 

    step(1)

    expect(m.io.done, false.B)

    // idle -> idle
    poke(m.io.ready, false.B)

    step(1)

    expect(m.io.done, false.B)
    // idle -> exec
    poke(m.io.ready, true.B)

    step(1)

    expect(m.io.done, true.B) 
    // here ideally I could test exec -> exec, but still the FSA always goes from exec -> idle
    poke(m.io.ready, true.B) 

    step(1)

    expect(m.io.done, false.B)




}
/*
    this test aims at verifying the correct behaviour of loads and stores
*/
class DoLoadStoreTest(m: CustomInterfacePETests) extends PeekPokeTester(m){
    // CK 0

    // do a store in memory (idle -> exec)
    // mem(2) := 20
    poke(m.io.cmd_funct, 1.U) 
    poke(m.io.cmd_rs1, 20.U)
    poke(m.io.cmd_rs2, 2.U)
    poke(m.io.ready, true.B)

    step(1)
    // CK 1

    expect(m.io.done , true.B)
    expect(m.io.resp_data, 20.U) // not really necessary

    // try to do a load but nothing happens since (exec -> idle)
    // 20 = mem(2)
    poke(m.io.cmd_funct, 0.U)
    poke(m.io.cmd_rs1, 2.U)
    poke(m.io.ready, false.B )
    
    step(1)
    // CK 2

    expect(m.io.done, false.B)
    expect(m.io.resp_data, 0.U)

    // try to do a load, and actually succeed
    // 20 = mem(2)
    poke(m.io.cmd_funct, 0.U)
    poke(m.io.cmd_rs1, 2.U)
    poke(m.io.ready, true.B)

    step(1)
    // CK 3

    expect(m.io.done, true.B)
    expect(m.io.resp_data, 20.U)

    // try to do a store, but fails since (exec -> idle)
    // mem(5) := 9
    poke(m.io.cmd_funct, 1.U) 
    poke(m.io.cmd_rs1, 9.U)
    poke(m.io.cmd_rs2, 5.U)
    poke(m.io.ready, true.B)

    step(1)
    // CK 4

    expect(m.io.done, false.B)
    expect(m.io.resp_data, 0.U)

    // verifies that the store didn't work, doing a load on the very same address
    // 20 = mem(2)
    poke(m.io.cmd_funct, 0.U)
    poke(m.io.cmd_rs1, 5.U)
    poke(m.io.ready, true.B)

    step(1)

    // CK 5

    expect(m.io.done, true.B)
    expect(m.io.resp_data, 0.U)

}


class PETests extends ChiselFlatSpec {

  val testerArgs = Array("")

  behavior of "StateMachineTest"
  it should "change state correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfacePETests()) {
      c => new StateMachineTest(c)
    } should be (true)
  }

  behavior of "DoLoadStoreTest"
  it should "execute loads and stores correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfacePETests()) {
      c => new DoLoadStoreTest(c)
    } should be (true)
  }
  
}