
package HPPS_Project
import java.io.ObjectInputFilter.Config

import freechips.rocketchip.config.{Config}


  class TorusAccelerator(opcodes: OpcodeSet)
    (implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new TorusAcceleratorModule(this)
}

class TorusAcceleratorModule(outer: TorusAccelerator)
    extends LazyRoCCModuleImp(outer) {
  val cmd = Queue(io.cmd)
  // The parts of the command are as follows
  // inst - the parts of the instruction itself
  //   opcode
  val resp = io.resp
  val rs1 = cmd.bits.rs1
  val rs2 = cmd.bits.rs2
  cmd.ready := true.B
  resp.valid := true.B
  resp.bits.rd := 3.U
  resp.bits.data := rs1 + rs2
    /// inputs of

  //   rd - destination register number
  //   rs1 - first source register number
  //   rs2 - second source register number
  //   funct
  //   xd - is the destination register being used?
  //   xs1 - is the first source register being used?
  //   xs2 - is the second source register being used?
  // rs1 - the value of source register 1
  // rs2 - the value of source register 2
  //...
}