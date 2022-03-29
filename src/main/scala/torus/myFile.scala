
package torus

import chisel3._
import chisel3.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.config.{Config, Parameters}
import chisel3.util.HasBlackBoxResource
import chisel3.experimental.IntParam
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.InOrderArbiter

class RoCCInstructionCustom extends Bundle {
  val funct = Bits(7.W)
  val rs2 = Bits(5.W)
  val rs1 = Bits(5.W)
  val xd = Bool()
  val xs1 = Bool()
  val xs2 = Bool()
  val rd = Bits(5.W)
  val opcode = Bits(7.W)
}

class RoCCCommandCustom(implicit p: Parameters) extends CoreBundle()(p) {
  val inst = new RoCCInstructionCustom
  val rs1 = Bits(xLen.W)
  val rs2 = Bits(xLen.W)
}

class RoCCResponseCustom(implicit p: Parameters) extends CoreBundle()(p) {
  val rd = Bits(5.W)
  val data = Bits(xLen.W)
}

class RoCCCoreIOCustom(implicit p: Parameters) extends CoreBundle()(p) {
  val cmd = Flipped(Decoupled(new RoCCCommandCustom)) // inputs : bits, valid (out : ready)
  val resp = Decoupled(new RoCCResponseCustom) // inputs : ready (out : bits, valid)
}

class LazyRoCCModuleImpCustom(implicit p: Parameters) extends Module{
  val io = IO(new RoCCCoreIOCustom())
}

class CustomInterface extends Module{
  val io = IO(new Bundle{
    val cmd_ready = Output(Bool())
    val cmd_valid = Input(Bool())
    val cmd_bits_rs1 = Input(Bits(32.W))
    val cmd_bits_rs2 = Input(Bits(32.W))
    val cmd_bits_instr_funct = Input(Bits(7.W))
    val cmd_bits_instr_rs1 = Input(Bits(5.W))
    val cmd_bits_instr_rs2 = Input(Bits(5.W))
    val cmd_bits_instr_xd = Input(Bool())
    val cmd_bits_instr_xs1 = Input(Bool())
    val cmd_bits_instr_xs2 = Input(Bool())
    val cmd_bits_instr_opcode = Input(Bits(7.W))
    val cmd_bits_instr_rd = Input(Bits(5.W))
  })

}

  class TorusAccelerator(opcodes: OpcodeSet) (implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new TorusAcceleratorModule(this)
}

class TorusAcceleratorModule(outer: TorusAccelerator) extends LazyRoCCModuleImp(outer) {
  val cmd = Queue(io.cmd)
  val myModuleImpl = new TorusAcceleratorModuleImpl()
  myModuleImpl.io.cmd.valid := cmd.valid
  myModuleImpl.io.cmd.bits.rs1 := cmd.bits.rs1
  myModuleImpl.io.cmd.bits.rs2 := cmd.bits.rs2
  myModuleImpl.io.cmd.bits.inst.funct := cmd.bits.inst.funct
  myModuleImpl.io.cmd.bits.inst.rs1 := cmd.bits.inst.rs1
  myModuleImpl.io.cmd.bits.inst.rs2 := cmd.bits.inst.rs2
  myModuleImpl.io.cmd.bits.inst.xd := cmd.bits.inst.xd
  myModuleImpl.io.cmd.bits.inst.xs1 := cmd.bits.inst.xs1
  myModuleImpl.io.cmd.bits.inst.xs2 := cmd.bits.inst.xs2
  myModuleImpl.io.cmd.bits.inst.rd := cmd.bits.inst.rd
  myModuleImpl.io.cmd.bits.inst.opcode := cmd.bits.inst.opcode
  myModuleImpl.io.resp.ready := io.resp.ready

  // my code which sets:
    // myModuleImpl.io.cmd.ready
    // myModuleImpl.io.resp.valid
    // myModuleImpl.io.resp.bits.rd
    // myModuleImpl.io.resp.bits.data

  cmd.ready := myModuleImpl.io.cmd.ready
  io.resp.valid := myModuleImpl.io.resp.valid 
  io.resp.bits.rd := myModuleImpl.io.resp.bits.rd
  io.resp.bits.data := myModuleImpl.io.resp.bits.data
  

  /*
  val resp = io.resp
  val rs1 = cmd.bits.rs1
  val rs2 = cmd.bits.rs2
  cmd.ready := true.B
  resp.valid := true.B
  resp.bits.rd := 3.U
  resp.bits.data := rs1 + rs2
  */
  
}

class TorusAcceleratorModuleImpl(implicit p: Parameters) extends LazyRoCCModuleImpCustom{

  val sum = io.cmd.bits.rs1 + io.cmd.bits.rs2
  io.cmd.ready := true.B
  io.resp.valid := true.B 
  io.resp.bits.rd := sum
  io.resp.bits.data := sum

}