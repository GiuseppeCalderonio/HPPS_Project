
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
class TorusAcceleratorModuleImpl(n : Int = 4) extends LazyRoCCModuleImpCustom{

  def module(i : Int ): Int = {
    if(i < 0) return i + n
    else if(i >= n) return i - n
    else return i
  }

  val controller = Module(new Controller)

  var temp = Seq[PE]()

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
  
    pe_i.io.cmd <> controller.io.cmd 
    //pe_i.io.ready := controller.io.ready
  
  })

  //controller.io.resp.data := pe.map(_.io.resp.data).reduce(_ & _) // does not make sense right now, just to test

  //controller.io.done := pe.map(_.io.done).reduce(_ && _) // done iif all the PEs are done

  /*
  pe.io.cmd <> controller.io.cmd 
  pe.io.resp <> controller.io.resp

  pe.io.ready := controller.io.ready

  // ideally here : done := PE1.done && PE2.done && .... 

  controller.io.done := pe.io.done*/
  

}