
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

    val t = Module(new Torus())


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

class ConnectionTest(m : CustomInterfaceTorusTest) extends PeekPokeTester(m){

  val load = 0
  val store = 1
  val get_load = 2
  var rd = 7

  // do a store: mem(rs2) := rs1, (rs1, rs2) = (3, 5) -> mem(5) := 3
  poke(m.io.cmd_bits_inst_opcode, "b0001011".U) // always true
  poke(m.io.cmd_valid, true.B )
  poke(m.io.cmd_bits_rs1, 3.U )
  poke(m.io.cmd_bits_rs2, 5.U )
  poke(m.io.cmd_bits_inst_funct, store.U )
  poke(m.io.cmd_bits_inst_rd, rd.U  )

  poke(m.io.resp_ready, true.B )
  
  step(1)

  expect(m.io.cmd_ready, true.B )

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_data, 0.U )
  expect(m.io.resp_bits_rd, rd.U )

  expect(m.io.busy, false.B )
  expect(m.io.interrupt, false.B )

  rd = 11

  // do a load: rd := mem(rs1), (rs1) = (5) -> rd := mem(5) = 3 (data still 0 since 3 is enqueued )
  poke(m.io.cmd_bits_inst_opcode, "b0001011".U) // always true
  poke(m.io.cmd_valid, true.B )
  poke(m.io.cmd_bits_rs1, 5.U )
  poke(m.io.cmd_bits_inst_funct, load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U  )

  poke(m.io.resp_ready, true.B )
  
  step(1)

  expect(m.io.cmd_ready, true.B )

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_data, 0.U )
  expect(m.io.resp_bits_rd, rd.U )

  expect(m.io.busy, false.B )
  expect(m.io.interrupt, false.B )


  rd = 13

  // do a get_load: data = out_queue, out_queue = [3, NULL, NULL] => data = 3
  poke(m.io.cmd_bits_inst_opcode, "b0001011".U) // always true
  poke(m.io.cmd_valid, true.B )
  poke(m.io.cmd_bits_inst_funct, get_load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U  )

  poke(m.io.resp_ready, true.B )
  
  step(1)

  expect(m.io.cmd_ready, true.B ) // in queue not full
  expect(m.io.resp_valid, false.B ) // out queue still empty

  step(1)

  expect(m.io.cmd_ready, true.B )

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_data, 3.U )
  expect(m.io.resp_bits_rd, rd.U )

  expect(m.io.busy, false.B )
  expect(m.io.interrupt, false.B )

}

class TorusTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  behavior of "ConnectionTest"
  it should "Tests that the connection of PEs with the controller works fine, doing a store, load and get_load" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceTorusTest()) {
      c => new ConnectionTest(c)
    } should be (true)
  }

}


