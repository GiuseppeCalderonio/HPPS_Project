
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


class Matrix(n: Int) extends LazyRoCCModuleImpCustom{


  def module(k: Int): Int = {
    val result = k % n
    result

  }

  val matrix = Array.ofDim[MatrixElement](n, n, n)

  //io.resp := ??

  // initialize the matrix
  /*
  for (i<-0 to n; j<-0 to n; k<-0 to n){
    matrix(i)(j)(k) := new MatrixElement
  }*/

  // connect the matrix with the core
  for (i<-0 to n; j<-0 to n; k<-0 to n){
    matrix(i)(j)(k).io.cmd <> io.cmd
  }

  for (i<-0 to n; j<-0 to n; k<-0 to n){
    matrix(i)(j)(k).io.c.up.out := matrix(module(i + 1))(j)(k).io.c.down.in
    matrix(i)(j)(k).io.c.down.out := matrix(module(i - 1))(j)(k).io.c.up.in
    matrix(i)(j)(k).io.c.left.out := matrix(i)(module(j + 1))(k).io.c.right.in
    matrix(i)(j)(k).io.c.right.out := matrix(i)(module(j + - 1))(k).io.c.left.in
    matrix(i)(j)(k).io.c.ingoing.out := matrix(i)(j)(module(k + 1)).io.c.outgoing.in
    matrix(i)(j)(k).io.c.outgoing.out := matrix(i)(j)(module(k - 1)).io.c.ingoing.in
  }



}

class TorusAcceleratorModuleImpl() extends LazyRoCCModuleImpCustom{

  // io is the interface extended

  val opcode = io.cmd.bits.inst.opcode
  val rd_address = io.cmd.bits.inst.rd
  val xs1 = io.cmd.bits.inst.xs1
  val xs2 = io.cmd.bits.inst.xs2
  val xd = io.cmd.bits.inst.xd 
  val rs1_address = io.cmd.bits.inst.rs1
  val rs2_address = io.cmd.bits.inst.rs2
  val funct = io.cmd.bits.inst.funct
  val in_valid = io.cmd.valid 
  val out_ready = io.resp.ready
  val rs1_content = io.cmd.bits.rs1 
  val rs2_content = io.cmd.bits.rs2

  val rd_buffer = Reg(Bits(5.W))
  val sum = Reg(Bits(32.W))

  val rd_stay = Wire(Bool())

  

  val state = Reg(Bits(2.W))

  val idle :: exec_sum :: give_result :: Nil = Enum(3)

  state := idle

  when(state === idle){
    io.busy := false.B 
    io.cmd.ready := true.B 
    io.resp.valid := false.B
    rd_stay := false.B 
    when(in_valid){
      state := exec_sum
    }.otherwise{
      state := idle
    }
  }.elsewhen(state === exec_sum){
    io.busy := true.B
    io.cmd.ready := false.B 
    io.resp.valid := false.B
    rd_stay := true.B 
    state := give_result
    
  }.elsewhen(state === give_result){
    io.busy := true.B
    io.cmd.ready := false.B 
    io.resp.valid := true.B
    rd_stay := true.B 
    state := idle
  }.otherwise{ // shouldn't happen
    
  io.busy := false.B
  io.cmd.ready := false.B 
  io.resp.valid := false.B
  rd_stay := false.B 
  state := idle
  
  }

  

  rd_buffer := Mux(rd_stay, rd_buffer, rd_address)
  sum := Mux(state === idle, rs1_content + rs2_content, sum)
  io.resp.bits.data := sum
  io.resp.bits.rd := rd_buffer

  io.interrupt := false.B

  
  
  



}

