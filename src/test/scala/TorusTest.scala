
package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class CustomInterface () extends Module{
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


    
  def connect(t : TorusAcceleratorModuleImpl) ={
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



  }
  
}





class MyFirstModuleTests(m: CustomInterface) extends PeekPokeTester(m){

    //m.connect(new TorusAcceleratorModuleImpl())

    poke(m.io.cmd_valid, true.B)
    poke(m.io.cmd_bits_rs1, 1.U)
    poke(m.io.cmd_bits_rs2, 1.U)
    poke(m.io.cmd_bits_inst_funct, 0.U)
    poke(m.io.cmd_bits_inst_rs1, 0.U)
    poke(m.io.cmd_bits_inst_rs2, 0.U)
    poke(m.io.cmd_bits_inst_xd, true.B)
    poke(m.io.cmd_bits_inst_xs1, true.B)
    poke(m.io.cmd_bits_inst_xs2, true.B)
    poke(m.io.cmd_bits_inst_rd, 0.U)
    poke(m.io.cmd_bits_inst_opcode, 0.U)
    poke(m.io.resp_ready, true.B)

    step(1)

    expect(m.io.cmd_ready, true.B)
    expect(m.io.resp_valid, true.B)
    expect(m.io.resp_bits_rd, 0.U) 
    expect(m.io.resp_bits_data, 2.U)

    //println("Success")



}

class TorusUnitTest extends ChiselFlatSpec {

  val testerArgs = Array( ""
    //"--backend-name", "treadle",
    // "--generate-vcd-output", "on",
    //"--target-dir", "test_run_dir/dmacommandtracker"
  )

  //val toConnect = new LazyRoCCModuleImpCustom()

  behavior of "Torus"
  it should "work" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterface()) {
      c => new MyFirstModuleTests(c)
    } should be (true)
  }
}