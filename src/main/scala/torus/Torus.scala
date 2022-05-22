
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
import freechips.rocketchip.util._

/*
// a scatch of implementation of the matrix connections
class Matrix(n: Int) extends LazyRoCCModuleImpCustom{


  def module(k: Int): Int = {
    val result = k % n
    result

  }

  val matrix = Array.ofDim[MatrixElement](n, n, n)

  //io.resp := ??

  // initialize the matrix
  
  for (i<-0 to n; j<-0 to n; k<-0 to n){
    matrix(i)(j)(k) := new MatrixElement
  }

  // connect the matrix with the core
  for (i<-0 to n; j<-0 to n; k<-0 to n){
    matrix(i)(j)(k).io.cmd <> io.cmd
  }

  for (i<-0 to n; j<-0 to n; k<-0 to n){
    matrix(i)(j)(k).io.c.up.out := matrix(module(i + 1))(j)(k).io.c.down.in
    matrix(i)(j)(k).io.c.down.out := matrix(module(i - 1))(j)(k).io.c.up.in
    matrix(i)(j)(k).io.c.left.out := matrix(i)(module(j + 1))(k).io.c.right.in
    matrix(i)(j)(k).io.c.right.out := matrix(i)(module(j - 1))(k).io.c.left.in
    matrix(i)(j)(k).io.c.ingoing.out := matrix(i)(j)(module(k + 1)).io.c.outgoing.in
    matrix(i)(j)(k).io.c.outgoing.out := matrix(i)(j)(module(k - 1)).io.c.ingoing.in
  }


  
}
*/

/*
this module is the "main" module, which connects and contains all the high level
components, so the controller with the set of PEs

*/
class Torus(n : Int = 2) extends LazyRoCCModuleImpCustom{

  def module(i : Int ): Int = {
    if(i < 0) return i + n
    else if(i >= n) return i - n
    else return i
  }

  val controller = Module(new Controller(5))

  var temp = Array.ofDim[PE](n, n, n)

  for (i<-0 until n; j<-0 until n; k<-0 until n){
    temp(i)(j)(k) = Module(new PE(i+j+k)) // id will change
  }

  val pe = temp

  val full_PE_cmd_ready = Wire(Bool())
  val full_PE_resp_valid = Wire(Bool())
  val full_PE_resp_bits_data = Wire(Bits(32.W))

  for (i<-0 until n; j<-0 until n; k<-0 until n){

    pe(i)(j)(k).io.cmd.valid := controller.io.cmd.valid 
    pe(i)(j)(k).io.cmd.bits.rs1 := controller.io.cmd.bits.rs1
    pe(i)(j)(k).io.cmd.bits.rs2 := controller.io.cmd.bits.rs2
    pe(i)(j)(k).io.cmd.bits.funct := controller.io.cmd.bits.funct
    pe(i)(j)(k).io.resp.ready := controller.io.resp.ready

    

    full_PE_cmd_ready := pe.flatMap( _.flatMap(_.map(_.io.cmd.ready))).reduce(_ & _) //.io.cmd.ready
    full_PE_resp_valid := pe.flatMap( _.flatMap(_.map(_.io.resp.valid))).reduce(_ & _) // .io.resp.valid 
    full_PE_resp_bits_data := pe.flatMap( _.flatMap(_.map(_.io.resp.bits.data))).reduce(_ & _) // .io.resp.bits.data

  }

  controller.io.cmd.ready := full_PE_cmd_ready
  controller.io.resp.valid := full_PE_resp_valid
  controller.io.resp.bits.data := full_PE_resp_bits_data

  for (i<-0 until n; j<-0 until n; k<-0 until n){
    pe(i)(j)(k).io.conn.right.in <> pe(module(i + 1))(j)(k).io.conn.left.out
    pe(i)(j)(k).io.conn.left.in <> pe(module(i - 1))(j)(k).io.conn.right.out
  }


  controller.io.rocc <> io

/*


  for(i <- 0 until n){
    temp = temp :+ Module(new PE(i))
  }

  val pe = temp

  
  for(i <- 0 until n){
    pe(i).io.conn.right.in <> pe(module(i + 1)).io.conn.left.out
    pe(i).io.conn.left.in <> pe(module(i - 1)).io.conn.right.out
  }

  controller.io.rocc <> io

  pe.seq.foreach( pe_i => {
  
    pe_i.io.cmd.valid := controller.io.cmd.valid 
    pe_i.io.cmd.bits.rs1 := controller.io.cmd.bits.rs1
    pe_i.io.cmd.bits.rs2 := controller.io.cmd.bits.rs2
    pe_i.io.cmd.bits.funct := controller.io.cmd.bits.funct
    pe_i.io.resp.ready := controller.io.resp.ready
  
  })

  controller.io.cmd.ready := pe.map(_.io.cmd.ready).reduce(_ & _)
  controller.io.resp.valid := pe.map(_.io.resp.valid).reduce(_ & _)
  controller.io.resp.bits.data := pe.map(_.io.resp.bits.data).reduce(_ & _)

  */
}