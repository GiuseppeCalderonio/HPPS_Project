
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
this module is the "main" module, which connects and contains all the high level
components, so the controller with the set of PEs

*/
class Torus(n : Int = 2, io_controller_queue_size : Int = 5, width : Int = 32, pe_neighbour_queue_size : Int = 3) extends LazyRoCCModuleImpCustom{

  def module(i : Int ): Int = {
    if(i < 0) return i + n
    else if(i >= n) return i - n
    else return i
  }

  def giveOrder(i : Int, j : Int): Int = {
    return i*n + j

  }

  val controller = Module(new Controller(io_controller_queue_size))

  var temp = Array.ofDim[PE](n, n)

  for (i<-0 until n; j<-0 until n){
    temp(i)(j) = Module(new PE(width, pe_neighbour_queue_size))
  }

  val pe = temp

  val arbiter = Module(new LoadArbiter(16, n*n))

  val full_PE_cmd_ready = Wire(Bool())
  val full_PE_resp_bits_data = Vec(n*n, Bits(16.W ))

  for (i<-0 until n; j<-0 until n){

    pe(i)(j).io.cmd.valid := controller.io.cmd.valid 
    pe(i)(j).io.cmd.bits.rs1 := controller.io.cmd.bits.rs1
    pe(i)(j).io.cmd.bits.rs2 := controller.io.cmd.bits.rs2
    pe(i)(j).io.cmd.bits.funct := controller.io.cmd.bits.funct
    pe(i)(j).io.resp.ready := arbiter.io.in_ready

    

    full_PE_cmd_ready := pe.flatMap(_.map(_.io.cmd.ready)).reduce(_ & _)
    arbiter.io.in_valid := pe.flatMap(_.map(_.io.resp.valid)).reduce(_ & _)
    arbiter.io.in_vec(giveOrder(i, j)) := pe(i)(j).io.resp.bits.data 

  }

  arbiter.io.out.ready := controller.resp.ready

  controller.io.cmd.ready := full_PE_cmd_ready
  controller.io.resp.valid := arbiter.io.out.valid
  controller.io.resp.bits.data := arbiter.io.out.bits.data

  for (i<-0 until n; j<-0 until n){
    pe(i)(j).io.conn.right.in <> pe(module(i + 1))(j).io.conn.left.out
    pe(i)(j).io.conn.left.in <> pe(module(i - 1))(j).io.conn.right.out
    pe(i)(j).io.conn.up.in <> pe(i)(module(j + 1)).io.conn.down.out
    pe(i)(j).io.conn.down.in <> pe(i)(module(j - 1)).io.conn.up.out
  }


  controller.io.rocc <> io

}