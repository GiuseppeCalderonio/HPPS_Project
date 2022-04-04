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

class RoCCCommandCustom() extends Bundle() {
  val inst = new RoCCInstructionCustom
  val rs1 = Bits(32.W)
  val rs2 = Bits(32.W)
}

class RoCCResponseCustom() extends Bundle() {
  val rd = Bits(5.W)
  val data = Bits(32.W)
}

class RoCCCoreIOCustom() extends Bundle() {
  val cmd = Flipped(Decoupled(new RoCCCommandCustom)) // inputs : bits, valid (out : ready)
  val resp = Decoupled(new RoCCResponseCustom) // inputs : ready (out : bits, valid)
  val interrupt = Output(Bool()) // always false
  val busy = Output(Bool()) // true when working

}

class LazyRoCCModuleImpCustom() extends Module{
  val io = IO(new RoCCCoreIOCustom())
}

///////////////////////connected design/////////////////////////////

  class TorusAccelerator(opcodes: OpcodeSet) (implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new TorusAcceleratorModule(this)
}

class TorusAcceleratorModule(outer: TorusAccelerator) extends LazyRoCCModuleImp(outer) {
  val cmd = Queue(io.cmd)
  val myModuleImpl = Module(new TorusAcceleratorModuleImpl())

  // inputs for my module
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

  // outputs for my module

  cmd.ready := myModuleImpl.io.cmd.ready
  io.resp.valid := myModuleImpl.io.resp.valid 
  io.resp.bits.rd := myModuleImpl.io.resp.bits.rd
  io.resp.bits.data := myModuleImpl.io.resp.bits.data
  io.interrupt := myModuleImpl.io.interrupt
  io.busy := myModuleImpl.io.busy
  
}