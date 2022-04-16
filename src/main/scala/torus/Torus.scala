
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


// a scatch of implementation of the matrix connections
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
/*
this module does the sum when funct === 0 rd := rs1 + rs2
this module does the load when funct === 1 rd := mem(rs1)
this module does the store when funct === 2 mem(rs2) := rs1

*/
class TorusAcceleratorModuleImpl(n : Int = 4) extends LazyRoCCModuleImpCustom{

  val controller = Module(new Controller)
  //val pe = Module(new PE(1))

  var temp : Seq[PE] = null

  for(i <- 0 until n){
    temp = temp :+ Module(new PE(i)) 
  }

  val pe = temp

  controller.io.rocc <> io

  pe.seq.foreach( pe_i => {
  
    pe_i.io.cmd <> controller.io.cmd
    pe_i.io.resp <> controller.io.resp
    pe_i.io.ready := controller.io.ready
  
  })

  controller.io.done := pe.map(_.io.done).reduce(_ && _)

  /*
  pe.io.cmd <> controller.io.cmd 
  pe.io.resp <> controller.io.resp

  pe.io.ready := controller.io.ready

  // ideally here : done := PE1.done && PE2.done && .... 

  controller.io.done := pe.io.done*/
  

}

