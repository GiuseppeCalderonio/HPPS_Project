
package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class CustomInterfaceControllerTest extends Module{

    val io = IO(new Bundle{

        val cmd_funct = Output(Bits(7.W))
        val cmd_opcode = Output(Bits(7.W))
        val cmd_rs1 = Output(Bits(32.W))
        val cmd_rs2 = Output(Bits(32.W))
        val resp_data = Input(Bits(32.W))
        val done = Input(Bool())
        val ready = Output(Bool())
        // rocc signals
        val rocc_cmd_ready = Output(Bool())
        val rocc_cmd_valid = Input(Bool())

        val rocc_cmd_bits_inst_funct = Input(Bits(7.W))
        val rocc_cmd_bits_inst_rs2 = Input(Bits(5.W))
        val rocc_cmd_bits_inst_rs1 = Input(Bits(5.W))
        val rocc_cmd_bits_inst_xd = Input(Bool())
        val rocc_cmd_bits_inst_xs1 = Input(Bool())
        val rocc_cmd_bits_inst_xs2 = Input(Bool())
        val rocc_cmd_bits_inst_rd = Input(Bits(5.W))
        val rocc_cmd_bits_inst_opcode = Input(Bits(7.W))

        val rocc_cmd_bits_rs1 = Input(Bits(32.W))
        val rocc_cmd_bits_rs2 = Input(Bits(32.W))

        val rocc_resp_ready = Input(Bool())
        val rocc_resp_valid = Output(Bool())
        val rocc_resp_bits_rd = Output(Bits(5.W))
        val rocc_resp_bits_data = Output(Bits(32.W))

        val rocc_interrupt = Output(Bool())
        val rocc_busy = Output(Bool())

    })

    val contr = Module(new Controller())

    // connecting inputs: contr = io.in

    contr.resp.data := io.resp_data
    contr.done := io.done
    contr.rocc.cmd.valid := io.rocc_cmd_valid
    contr.rocc.cmd.bits.inst.funct := io.rocc_cmd_bits_inst_funct
    contr.rocc.cmd.bits.inst.rs2 := io.rocc_cmd_bits_inst_rs2
    contr.rocc.cmd.bits.inst.rs1 := io.rocc_cmd_bits_inst_rs1
    contr.rocc.cmd.bits.rs2 := io.rocc_cmd_bits_rs2
    contr.rocc.cmd.bits.rs1 := io.rocc_cmd_bits_rs1
    contr.rocc.cmd.bits.inst.xd := io.rocc_cmd_bits_inst_xd
    contr.rocc.cmd.bits.inst.xs1 := io.rocc_cmd_bits_inst_xs1
    contr.rocc.cmd.bits.inst.xs2 := io.rocc_cmd_bits_inst_xs2
    contr.rocc.cmd.bits.inst.rd := io.rocc_cmd_bits_inst_rd
    contr.rocc.cmd.bits.inst.opcode := io.rocc_cmd_bits_inst_opcode
    contr.rocc.resp.ready := io.rocc_resp_ready

    // connecting outputs: io.out = contr

    io.rocc_busy := contr.rocc.busy
    io.cmd_funct := contr.cmd.funct
    io.cmd_rs1 := contr.cmd.rs1
    io.cmd_rs2 := contr.cmd.rs2
    io.cmd_opcode := contr.cmd.opcode
    io.ready := contr.ready
    io.rocc_cmd_ready := contr.rocc.cmd.ready
    io.rocc_resp_valid := contr.rocc.resp.valid
    io.rocc_resp_bits_rd := contr.rocc.resp.bits.rd
    io.rocc_resp_bits_data := contr.rocc.resp.bits.data
    io.rocc_interrupt := contr.rocc.interrupt
}



class StateMachineTestAndRd(m : CustomInterfaceControllerTest) extends PeekPokeTester(m){

    // idle

    poke(m.io.rocc_cmd_valid, false.B)
    poke(m.io.rocc_cmd_bits_inst_rd, 13.U)

    step(1) // idle -> idle

    expect(m.io.rocc_busy, false.B)
    expect(m.io.rocc_cmd_ready, true.B)
    expect(m.io.rocc_resp_valid, false.B)
    expect(m.io.ready, false.B)

    // idle

    poke(m.io.rocc_cmd_valid, true.B)
    poke(m.io.rocc_cmd_bits_inst_rd, 3.U)

    step(1) // idle -> exec

    expect(m.io.rocc_busy, true.B)
    expect(m.io.rocc_cmd_ready, false.B)
    expect(m.io.rocc_resp_valid, false.B)
    expect(m.io.ready, true.B)

    for(i <- 0 to 3){
        // exec
        poke(m.io.done, false.B)
        poke(m.io.rocc_cmd_bits_inst_rd, 8.U)

        step(1) // exec -> exec

        expect(m.io.rocc_busy, true.B)
        expect(m.io.rocc_cmd_ready, false.B)
        expect(m.io.rocc_resp_valid, false.B)
        expect(m.io.ready, true.B)
    }

    //exec
    poke(m.io.done, true.B)
    poke(m.io.rocc_resp_ready, false.B)
    poke(m.io.rocc_cmd_bits_inst_rd, 12.U)

    step(1) // exec -> wait_result

    expect(m.io.rocc_resp_bits_rd, 3.U)
    expect(m.io.rocc_busy, true.B)
    expect(m.io.rocc_cmd_ready, false.B)
    expect(m.io.rocc_resp_valid, true.B)
    expect(m.io.ready, false.B)


    for(i <- 0 to 3){
        // wait_result
        poke(m.io.rocc_resp_ready, false.B)
        poke(m.io.rocc_cmd_bits_inst_rd, 8.U)

        step(1) // wait_result -> wait_result

        expect(m.io.rocc_resp_bits_rd, 3.U)

        expect(m.io.rocc_busy, true.B)
        expect(m.io.rocc_cmd_ready, false.B)
        expect(m.io.rocc_resp_valid, true.B)
        expect(m.io.ready, false.B)
    }

    // wait_result
    poke(m.io.rocc_resp_ready, true.B)

    step(1) // wait_result -> give_result

    expect(m.io.rocc_resp_bits_rd, 3.U)

    expect(m.io.rocc_busy, true.B)
    expect(m.io.rocc_cmd_ready, false.B)
    expect(m.io.rocc_resp_valid, true.B)
    expect(m.io.ready, false.B)

    // give_result

    step(1) // give_result -> idle

    expect(m.io.rocc_busy, false.B)
    expect(m.io.rocc_cmd_ready, true.B)
    expect(m.io.rocc_resp_valid, false.B)
    expect(m.io.ready, false.B)

    // idle

    poke(m.io.rocc_cmd_valid, true.B)
    poke(m.io.rocc_cmd_bits_inst_rd, 4.U)

    step(1) // idle -> exec

    poke(m.io.done , true.B )
    poke(m.io.rocc_resp_ready, true.B )

    step(1) // exec -> give_result

    expect(m.io.rocc_resp_bits_rd, 4.U)

    expect(m.io.rocc_busy, true.B)
    expect(m.io.rocc_cmd_ready, false.B)
    expect(m.io.rocc_resp_valid, true.B)
    expect(m.io.ready, false.B)

    // give_result

    step(1) // give_result -> idle

    expect(m.io.rocc_busy, false.B)
    expect(m.io.rocc_cmd_ready, true.B)
    expect(m.io.rocc_resp_valid, false.B)
    expect(m.io.ready, false.B)

}


class BusyTest (m : CustomInterfaceControllerTest) extends PeekPokeTester(m){

  // idle 

  for(i <- 0 to 3){
    // idle

    poke(m.io.rocc_cmd_valid, false.B)

    step(1) // idle -> idle

    expect(m.io.rocc_busy, false.B)
    expect(m.io.rocc_interrupt, false.B )

  }

  // idle

  poke(m.io.rocc_cmd_valid, true.B)

  step(1) // idle -> exec

  expect(m.io.rocc_busy, true.B )
  expect(m.io.rocc_interrupt, false.B )

  // exec

  for(i <- 0 to 3){

    // exec

    poke(m.io.rocc_cmd_valid, true.B)
    poke(m.io.done, false.B)


    step(1) // exec -> exec

    expect(m.io.rocc_busy, true.B)
    expect(m.io.rocc_interrupt, false.B )

  }

  // exec

  poke(m.io.done, true.B )
  poke(m.io.rocc_resp_ready, false.B )

  step(1) // exec -> wait_result 

  expect(m.io.rocc_busy, true.B)
  expect(m.io.rocc_interrupt, false.B )

  for(i <- 0 to 3){

    // wait_result

    poke(m.io.rocc_resp_ready, false.B)

    step(1) // wait_result -> wait_result

    expect(m.io.rocc_busy, true.B)
    expect(m.io.rocc_interrupt, false.B )
  }

  // wait_result

  poke(m.io.rocc_resp_ready, true.B)

  step(1) // wait_result -> give_result

  expect(m.io.rocc_busy, true.B)
  expect(m.io.rocc_interrupt, false.B )

  // give_result

  step(1) // give_result -> idle

  expect(m.io.rocc_busy, false.B )
  expect(m.io.rocc_interrupt, false.B )

}


class FilterInputSignalsTest(m: CustomInterfaceControllerTest) extends PeekPokeTester(m){

  var a = 0

  // idle

  poke(m.io.rocc_cmd_bits_inst_funct, a.U)
  poke(m.io.rocc_cmd_bits_inst_rd, a.U)
  poke(m.io.rocc_cmd_bits_inst_opcode,a.U)
  poke(m.io.rocc_cmd_bits_rs1, a.U)
  poke(m.io.rocc_cmd_bits_rs2, a.U)

  poke(m.io.rocc_cmd_valid, false.B)


  step(1) // idle-> idle 

  expect(m.io.cmd_funct, a.U)
  expect(m.io.cmd_opcode,a.U)
  expect(m.io.cmd_rs1, a.U)
  expect(m.io.cmd_rs2, a.U)

  a = a + 1

  // idle

  poke(m.io.rocc_cmd_bits_inst_funct, a.U)
  poke(m.io.rocc_cmd_bits_inst_rd, a.U)
  poke(m.io.rocc_cmd_bits_inst_opcode,a.U)
  poke(m.io.rocc_cmd_bits_rs1, a.U)
  poke(m.io.rocc_cmd_bits_rs2, a.U)

  poke(m.io.rocc_cmd_valid, true.B)


  step(1) // idle-> exec

  expect(m.io.cmd_funct, a.U)
  expect(m.io.cmd_opcode,a.U)
  expect(m.io.cmd_rs1, a.U)
  expect(m.io.cmd_rs2, a.U)

  for(a <- 0 to 3){

    // exec

    poke(m.io.rocc_cmd_bits_inst_funct, a.U)
    poke(m.io.rocc_cmd_bits_inst_rd, a.U)
    poke(m.io.rocc_cmd_bits_inst_opcode,a.U)
    poke(m.io.rocc_cmd_bits_rs1, a.U)
    poke(m.io.rocc_cmd_bits_rs2, a.U)

    poke(m.io.done, false.B)


    step(1) // exec -> exec

    expect(m.io.cmd_funct, a.U)
    expect(m.io.cmd_opcode,a.U)
    expect(m.io.cmd_rs1, a.U)
    expect(m.io.cmd_rs2, a.U)

  }

  a = a + 1

  // exec

  poke(m.io.rocc_cmd_bits_inst_funct, a.U)
  poke(m.io.rocc_cmd_bits_inst_rd, a.U)
  poke(m.io.rocc_cmd_bits_inst_opcode,a.U)
  poke(m.io.rocc_cmd_bits_rs1, a.U)
  poke(m.io.rocc_cmd_bits_rs2, a.U)

  poke(m.io.done, true.B)
  poke(m.io.rocc_resp_ready, false.B)


  step(1) // exec -> wait_result

  expect(m.io.cmd_funct, a.U)
  expect(m.io.cmd_opcode,a.U)
  expect(m.io.cmd_rs1, a.U)
  expect(m.io.cmd_rs2, a.U)

  expect(m.io.rocc_resp_bits_rd, 1.U)


  for(i <- 0 to 3){

    // wait_result

    a = a + 1

    poke(m.io.rocc_cmd_bits_inst_funct, i.U)
    poke(m.io.rocc_cmd_bits_inst_rd, i.U)
    poke(m.io.rocc_cmd_bits_inst_opcode, i.U)
    poke(m.io.rocc_cmd_bits_rs1, i.U)
    poke(m.io.rocc_cmd_bits_rs2, i.U)

    poke(m.io.rocc_resp_ready, false.B) 

    step(1) // wait_result -> wait_result

    expect(m.io.cmd_funct, i.U)
    expect(m.io.cmd_opcode, i.U)
    expect(m.io.cmd_rs1, i.U)
    expect(m.io.cmd_rs2, i.U)

  }

  a = a + 1

  // wait_result

    poke(m.io.rocc_cmd_bits_inst_funct, a.U)
    poke(m.io.rocc_cmd_bits_inst_rd, a.U)
    poke(m.io.rocc_cmd_bits_inst_opcode, a.U)
    poke(m.io.rocc_cmd_bits_rs1, a.U)
    poke(m.io.rocc_cmd_bits_rs2, a.U)

    poke(m.io.rocc_resp_ready, true.B) 

    step(1) // wait_result -> give_result

    expect(m.io.cmd_funct, a.U)
    expect(m.io.cmd_opcode, a.U)
    expect(m.io.cmd_rs1, a.U)
    expect(m.io.cmd_rs2, a.U)

    a = a + 1 

    // give_result

    poke(m.io.rocc_cmd_bits_inst_funct, a.U)
    poke(m.io.rocc_cmd_bits_inst_rd, a.U)
    poke(m.io.rocc_cmd_bits_inst_opcode, a.U)
    poke(m.io.rocc_cmd_bits_rs1, a.U)
    poke(m.io.rocc_cmd_bits_rs2, a.U)

    step(1) // give_result -> idle

    expect(m.io.cmd_funct, a.U)
    expect(m.io.cmd_opcode, a.U)
    expect(m.io.cmd_rs1, a.U)
    expect(m.io.cmd_rs2, a.U)

}

class FilterOutputSignalsTest(m : CustomInterfaceControllerTest) extends PeekPokeTester(m){


  var a = 0

  // idle

  poke(m.io.resp_data, a.U)
  poke(m.io.rocc_cmd_valid, false.B)


  step(1) // idle-> idle 

  expect(m.io.rocc_resp_bits_data, a.U)
  expect(m.io.rocc_resp_valid, false.B )

  a = a + 1

  // idle

  poke(m.io.resp_data, a.U)

  poke(m.io.rocc_cmd_valid, true.B)


  step(1) // idle-> exec 

  expect(m.io.rocc_resp_bits_data, a.U)
  expect(m.io.rocc_resp_valid, false.B )

  for(a <- 0 to 3){

    // exec

    poke(m.io.resp_data, a.U)
    poke(m.io.done, false.B)


    step(1) // exec -> exec

    expect(m.io.rocc_resp_bits_data, a.U)
    expect(m.io.rocc_resp_valid, false.B )

  }


  // exec

  poke(m.io.resp_data, 20.U)
  poke(m.io.done, true.B)
  poke(m.io.rocc_resp_ready, false.B)

  step(1) // exec -> wait_result

  expect(m.io.rocc_resp_bits_data, 20.U)
  expect(m.io.rocc_resp_valid, true.B)

  for(i <- 0 to 3){

    // wait_result

    poke(m.io.resp_data, i.U)
    poke(m.io.rocc_resp_ready, false.B)

    step(1) // wait_result -> wait_result

    expect(m.io.rocc_resp_bits_data, 20.U)
    expect(m.io.rocc_resp_valid, true.B )

  }

  // wait_result

  poke(m.io.resp_data, 30.U)
  poke(m.io.rocc_resp_ready, true.B)

  step(1) // wait_result -> give_result

  expect(m.io.rocc_resp_bits_data, 20.U)
  expect(m.io.rocc_resp_valid, true.B)

  // give_result

  poke(m.io.resp_data, 40.U)

  step(1) // give_result -> idle

  expect(m.io.rocc_resp_bits_data, 40.U)
  expect(m.io.rocc_resp_valid, false.B)


  // -------------------------------------- now the same, but with immediate return of result

  // idle

  a = 0

  poke(m.io.resp_data, a.U)
  poke(m.io.rocc_cmd_valid, false.B)


  step(1) // idle-> idle 

  expect(m.io.rocc_resp_bits_data, a.U)
  expect(m.io.rocc_resp_valid, false.B )

  a = a + 1

  // idle

  poke(m.io.resp_data, a.U)

  poke(m.io.rocc_cmd_valid, true.B)


  step(1) // idle-> exec 

  expect(m.io.rocc_resp_bits_data, a.U)
  expect(m.io.rocc_resp_valid, false.B )

  for(a <- 0 to 3){

    // exec

    poke(m.io.resp_data, a.U)
    poke(m.io.done, false.B)


    step(1) // exec -> exec

    expect(m.io.rocc_resp_bits_data, a.U)
    expect(m.io.rocc_resp_valid, false.B )

  }


  // exec

  poke(m.io.resp_data, 20.U)
  poke(m.io.done, true.B)
  poke(m.io.rocc_resp_ready, true.B)

  step(1) // exec -> give_result

  expect(m.io.rocc_resp_bits_data, 20.U)
  expect(m.io.rocc_resp_valid, true.B)

  // give_result

  poke(m.io.resp_data, 40.U)

  step(1) // give_result -> idle

  expect(m.io.rocc_resp_bits_data, 40.U)
  expect(m.io.rocc_resp_valid, false.B)
}



class ControllerTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  behavior of "StateMachineTestAndRd"
  it should "change state correctly, and gives back rd correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest()) {
      c => new StateMachineTestAndRd(c)
    } should be (true)
  }

  behavior of "BusyTest"
  it should "be Busy while in exec state, and interrupt always false" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest()) {
      c => new BusyTest(c)
    } should be (true)
  }

  behavior of "FilterInputSignalsTest"
  it should "Filter the right signals when given in input from rocc to cmd" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest()) {
      c => new FilterInputSignalsTest(c)
    } should be (true)
  }

  behavior of "FilterOutputSignalsTest"
  it should "Filter the right signals when given in output from resp to rocc" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceControllerTest()) {
      c => new FilterOutputSignalsTest(c)
    } should be (true)
  }  
}