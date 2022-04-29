package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._

/*
PE_state_machine interface
    val cmd_ready = Output(Bool()) // am I ready to receive new inputs?
    val cmd_valid = Input(Bool()) // is the new input received valid?
    val resp_ready = Input(Bool()) // is my output interface ready to receive commands?
    val resp_valid = Output(Bool()) // am I able to give a valid output in this cycle?
    val keep = Output(Bool()) // should I buffer my result or keep the execution flowing?

states:
    reg_free
    full_reg && io.resp_ready
    full_reg && !io.resp_ready

*/
class StateMachineTest(m: PE_control_unit) extends PeekPokeTester(m){

    // reg_free
    poke(m.io.cmd_valid, false.B )
    poke(m.io.resp_ready, true.B )

    step(1) // reg_free -> reg_free

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, false.B )
    expect(m.io.keep, false.B )

    // reg_free
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )

    step(1) // reg_free -> full_reg

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, false.B )

    // full_reg
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )

    step(1) // full_reg -> full_reg

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, false.B )

    // full_reg
    poke(m.io.cmd_valid, false.B )
    poke(m.io.resp_ready, false.B )

    step(1) // full_reg -> full_reg_stall

    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, true.B )

    // full_reg_stall
    poke(m.io.cmd_valid, false.B )
    poke(m.io.resp_ready, false.B )

    step(1) // full_reg_stall -> full_reg_stall

    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, true.B )

    // full_reg_stall
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )

    step(1) // full_reg_stall -> full_reg

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, false.B )

    // full_reg
    poke(m.io.cmd_valid, false.B )
    poke(m.io.resp_ready, true.B )

    step(1) // full_reg -> end_pipeline

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, false.B )

    step(1) // end_pipeline -> reg_free

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, false.B )
    expect(m.io.keep, false.B )

    //////////////////now th same but in different order///////////////////////////////////////

    // reg_free
    poke(m.io.cmd_valid, false.B )
    poke(m.io.resp_ready, true.B )

    step(1) // reg_free -> reg_free

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, false.B )
    expect(m.io.keep, false.B )

    // reg_free
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, false.B )

    step(1) // reg_free -> full_reg_stall

    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, true.B )

    // full_reg_stall
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, false.B )

    step(1) // full_reg_stall -> full_reg_stall

    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, true.B )

    // full_reg_stall
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )

    step(1) // full_reg_stall -> full_reg

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, false.B )

    // full_reg
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, false.B )

    step(1) // full_reg -> full_reg_stall

    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, true.B )

    // full_reg_stall
    poke(m.io.cmd_valid, false.B )
    poke(m.io.resp_ready, true.B )

    step(1) // full_reg_stall -> end_pipeline

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, true.B )
    expect(m.io.keep, false.B )

    // end_pipeiline

    step(1) // end_pipeline -> reg_free

    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, false.B )
    expect(m.io.keep, false.B )

}


class PEStateMachineTests extends ChiselFlatSpec {

  val testerArgs = Array("")

  behavior of "StateMachineTest"
  it should "change state correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE_control_unit()) {
      c => new StateMachineTest(c)
    } should be (true)
  }
  
}