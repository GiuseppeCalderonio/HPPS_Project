
package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class CustomInterfaceTorusTest () extends Module{
  val io = IO(new Bundle{
    val cmd_ready = Output(Bool())
    val cmd_valid = Input(Bool())
    val cmd_bits_rs1 = Input(Bits(32.W))
    val cmd_bits_rs2 = Input(Bits(32.W))
    val cmd_bits_inst_funct = Input(Bits(7.W))
    val cmd_bits_inst_rs1 = Input(Bits(5.W))
    val cmd_bits_inst_rs2 = Input(Bits(5.W))
    val cmd_bits_inst_xd = Input(Bool())
    val cmd_bits_inst_xs1 = Input(Bool())
    val cmd_bits_inst_xs2 = Input(Bool())
    val cmd_bits_inst_opcode = Input(Bits(7.W))
    val cmd_bits_inst_rd = Input(Bits(5.W))
    val resp_ready = Input(Bool())
    val resp_valid = Output(Bool())
    val resp_bits_data = Output(Bits(32.W))
    val resp_bits_rd = Output(Bits(5.W))
    val interrupt = Output(Bool())
    val busy = Output(Bool())
  })

    val t = Module(new TorusAcceleratorModuleImpl())


    // connecting inputs to module inputs
      // so from test -> cmd_valid -> t.io.cmd.valid -> (exec...) -> outputs
    t.io.cmd.valid := io.cmd_valid
    t.io.cmd.bits.rs1 := io.cmd_bits_rs1
    t.io.cmd.bits.rs2 := io.cmd_bits_rs2
    t.io.cmd.bits.inst.funct := io.cmd_bits_inst_funct
    t.io.cmd.bits.inst.rs1 := io.cmd_bits_inst_rs1
    t.io.cmd.bits.inst.rs2 := io.cmd_bits_inst_rs2
    t.io.cmd.bits.inst.xd := io.cmd_bits_inst_xd
    t.io.cmd.bits.inst.xs1 := io.cmd_bits_inst_xs1
    t.io.cmd.bits.inst.xs2 := io.cmd_bits_inst_xs2
    t.io.cmd.bits.inst.rd := io.cmd_bits_inst_rd
    t.io.cmd.bits.inst.opcode := io.cmd_bits_inst_opcode
    t.io.resp.ready := io.resp_ready

    // connecting outputs to module outputs
    // so from (exec...) -> t.io.resp.valid -> io.resp.valid -> test 

    io.cmd_ready := t.io.cmd.ready
    io.resp_valid := t.io.resp.valid 
    io.resp_bits_rd := t.io.resp.bits.rd
    io.resp_bits_data := t.io.resp.bits.data
    io.interrupt := t.io.interrupt
    io.busy := t.io.busy

    
  
}

class LogicHandlerTest(m: CustomInterfaceTorusTest) extends PeekPokeTester(m){

  
  var loop = 2

  //------------first with immediate results


  poke(m.io.cmd_valid, false.B )
  poke(m.io.cmd_bits_inst_opcode, 2.U )

  step(1) // no changes at all

  expect(m.io.interrupt, false.B )
  expect(m.io.busy, false.B )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_valid, false.B )


  // controller: idle, PE: idle
  poke(m.io.cmd_valid, true.B )
  poke(m.io.cmd_bits_inst_opcode, "b0001011".U )

  for(i <- 0 until loop){

    poke(m.io.resp_ready, true.B )

    step(1) // CK 1 & 2

    expect(m.io.interrupt, false.B )
    expect(m.io.busy, true.B )
    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, false.B )

  }

  // controller: exec, PE: exec

  poke(m.io.resp_ready, true.B )

  step(1) // CK 3

  expect(m.io.interrupt, false.B )
  expect(m.io.busy, true.B )
  expect(m.io.cmd_ready, false.B )
  expect(m.io.resp_valid, true.B )

  // controller: give_result, PE: idle

  step(1) // CK 4

  expect(m.io.interrupt, false.B )
  expect(m.io.busy, false.B )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_valid, false.B )


  //------------- then with a waiting condition

  // controller: idle, PE: idle
  poke(m.io.cmd_valid, true.B )
  poke(m.io.cmd_bits_inst_opcode, "b0001011".U )

  for(i <- 0 until loop){

    poke(m.io.resp_ready, false.B )

    step(1) // CK 1 & 2

    expect(m.io.interrupt, false.B )
    expect(m.io.busy, true.B )
    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, false.B )

  }

  for(i <- 0 until loop){

    poke(m.io.resp_ready, false.B )

    step(1) // CK 1 & 2

    expect(m.io.interrupt, false.B )
    expect(m.io.busy, true.B )
    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, true.B )

  }

  // controller: exec, PE: exec

  poke(m.io.resp_ready, true.B )

  step(1) // CK 3

  expect(m.io.interrupt, false.B )
  expect(m.io.busy, true.B )
  expect(m.io.cmd_ready, false.B )
  expect(m.io.resp_valid, true.B )

  // controller: give_result, PE: idle

  step(1) // CK 4

  expect(m.io.interrupt, false.B )
  expect(m.io.busy, false.B )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_valid, false.B )


}

class LoadStoreTest(m : CustomInterfaceTorusTest) extends PeekPokeTester(m){

  var rd = 3
  var rs1 = 15
  var rs2 = 26
  var load = 0
  var store = 1
  var load_result = rs1

  // do a store in all the PEs: mem(rs2) := rs1

  poke(m.io.cmd_valid, true.B )
  poke(m.io.cmd_bits_inst_funct, store.U )
  poke(m.io.cmd_bits_inst_opcode, "b0001011".U )
  poke(m.io.cmd_bits_rs1, rs1.U )
  poke(m.io.cmd_bits_rs2, rs2.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )
  poke(m.io.resp_ready, true.B )

  step(3) // it takes 3 CK to have the result ready

  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.resp_bits_data, rs1.U )

  step(1)

  rd = 10
  rs1 = rs2


  // do a load in all the PEs: data := mem(rs1)

  poke(m.io.cmd_valid, true.B )
  poke(m.io.cmd_bits_inst_funct, load.U )
  poke(m.io.cmd_bits_inst_opcode, "b0001011".U )
  poke(m.io.cmd_bits_rs1, rs1.U )
  poke(m.io.cmd_bits_rs2, rs2.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )
  poke(m.io.resp_ready, true.B )

  step(3)

  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.resp_bits_data, load_result.U )

}


class TorusTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  behavior of "LogicHandlerTest"
  it should "handle the logic signals correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceTorusTest()) {
      c => new LogicHandlerTest(c)
    } should be (true)
  }


  behavior of "LoadStoreTest"
  it should "execute loads and stores correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceTorusTest()) {
      c => new LoadStoreTest(c)
    } should be (true)
  }

}


