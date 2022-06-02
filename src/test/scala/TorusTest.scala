
package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class CustomInterfaceTorusTest (n: Int) extends Module{
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
    val cmd_bits_inst_rd = Input(Bits(5.W))
    val resp_ready = Input(Bool())
    val resp_valid = Output(Bool())
    val resp_bits_data = Output(Bits(32.W))
    val resp_bits_rd = Output(Bits(5.W))
    val interrupt = Output(Bool())
    val busy = Output(Bool())
  })

    val t = Module(new Torus(n))


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
    t.io.cmd.bits.inst.opcode := "b0001011".U
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

class LoadTest(m : CustomInterfaceTorusTest, n : Int) extends PeekPokeTester(m){

  val load = 0
  val store = 1
  val get_load = 2
  val exchange = 3
  var rd = 7
  var clock = 0
  var snapshot = 0

  println("---------START TEST, n_pes = " + n + "----------")

  // do a store on the 2nd memory mem2(10) := 30

  println( "First store started at clock: " +clock)
  snapshot = clock

  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 30.U )
  poke(m.io.cmd_bits_rs2, (10 + (2 << 16)).U )
  poke(m.io.cmd_bits_inst_funct, store.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)
  clock+=1
  println( "First store completed at clock: " +clock + ", CK = " + (clock - snapshot))

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  // do a load in the 2nd memory: data := mem2(10) (= 30)
  println( "First load started at clock: " +clock)
  snapshot = clock

  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 0.U )
  poke(m.io.cmd_bits_rs2, (10 + (2 << 16)).U )
  poke(m.io.cmd_bits_inst_funct, load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)
  clock+=1
  println(clock.toString())

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  println( "First load completed at clock: " +clock + ", CK = " + (clock - snapshot))

  // do 4 get_load and verify that the values were actually stored and then loaded
  println( "First get_load started at clock: " +clock)
  snapshot = clock

  for ( i <- 0 until n){
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )
    poke(m.io.cmd_bits_rs1, 0.U )
    poke(m.io.cmd_bits_rs2, 0.U )
    poke(m.io.cmd_bits_inst_funct, get_load.U )
    poke(m.io.cmd_bits_inst_rd, rd.U )

    step(1)
    clock+=1
    println(clock.toString())

    expect(m.io.resp_valid, true.B )
    expect(m.io.resp_bits_rd, rd.U )
    expect(m.io.resp_bits_data, 30.U )
    expect(m.io.cmd_ready, true.B )
  }

  println( "First get_load completed at clock: " +clock + ", CK = " + (clock - snapshot))

  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_inst_funct, get_load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)
  clock+=1
  println(clock.toString())

  expect(m.io.resp_valid, false.B )
  expect(m.io.cmd_ready, true.B )

  println("-------------------END OF THE TEST---------")

}

class ExchangeDataTest(m : CustomInterfaceTorusTest, n : Int, n_data : Int) extends PeekPokeTester(m){

  val load = 0
  val store = 1
  val get_load = 2
  val exchange = 3
  var rd = 7
  var clock = 0
  var snapshot = 0

  println("---------START TEST, n_pes = " + n + ", n_data = " + n_data + "----------")

  // do n_data store on the main memory memM(10 + i) := 30 + i

  println("start doing " + n_data + " stores at clock: " + clock)
  snapshot = clock

  for(i <- 0 until n_data){
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )
    poke(m.io.cmd_bits_rs1, (30 + i).U )
    poke(m.io.cmd_bits_rs2, (10 + i).U )
    poke(m.io.cmd_bits_inst_funct, store.U )
    poke(m.io.cmd_bits_inst_rd, rd.U )

    step(1)
    clock+=1
    println(clock.toString())

    expect(m.io.resp_valid, true.B )
    expect(m.io.resp_bits_rd, rd.U )
    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_bits_data, 13.U )
  }

  println("End doing " + n_data + " stores at clock: " + clock + ", CK = " + (clock - snapshot))

  // do a data exchange(src = 10, n = 3, dest = 40)

  println("start doing data exhange of " + n_data + " data at clock: " + clock)
  snapshot = clock

  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, (10 + (n_data << 16)).U )
  poke(m.io.cmd_bits_rs2, 40.U )
  poke(m.io.cmd_bits_inst_funct, exchange.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)
  clock+=1
  println(clock.toString())

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  poke(m.io.cmd_valid, false.B ) // this is false, rest is useess
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 0.U )
  poke(m.io.cmd_bits_rs2, (40 + (2 << 16)).U ) // do 3 loads: res := mem2(40 + i) (= 30 + i)
  poke(m.io.cmd_bits_inst_funct, load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )



  for( i <- 1 until n_data){

    step(1)
    clock+=1
    println(clock.toString())

    expect(m.io.resp_valid, false.B )
    expect(m.io.resp_bits_rd, rd.U )
    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_bits_data, 13.U )

    poke(m.io.cmd_valid, false.B ) // this is false, rest is useess
    poke(m.io.resp_ready, true.B )
    poke(m.io.cmd_bits_rs1, 0.U )
    poke(m.io.cmd_bits_rs2, (40 + i + (2 << 16)).U ) // do 3 loads: res := mem2(40 + i) (= 30 + i)
    poke(m.io.cmd_bits_inst_funct, load.U )
    poke(m.io.cmd_bits_inst_rd, rd.U )

  }

  // exchange finished, now it's time to do the loads and retireve their content with get_loads

  step(1)
  clock+=1
  println("End doing data exhange of " + n_data + " data at clock: " + clock + ", CK = " + (clock - snapshot))

  expect(m.io.resp_valid, false.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  println("From now on the test will check only the correctness of the memory after the excahnge")

  for(x <- 0 until n_data){
    for (mem <- 1 to 4){

      poke(m.io.cmd_valid, true.B ) // this is false, rest is useess
      poke(m.io.resp_ready, true.B )
      poke(m.io.cmd_bits_rs1, 0.U )
      poke(m.io.cmd_bits_rs2, (40 + x + (mem << 16)).U ) // do 3 loads: res := mem2(40 + i) (= 30 + i)
      poke(m.io.cmd_bits_inst_funct, load.U )
      poke(m.io.cmd_bits_inst_rd, rd.U )

      step(1)
      clock+=1
      //println(clock.toString() )


      expect(m.io.resp_valid, true.B )
      expect(m.io.resp_bits_rd, rd.U )
      expect(m.io.cmd_ready, true.B )
      expect(m.io.resp_bits_data, 13.U )

      for(i <- 0 until n){

        poke(m.io.cmd_valid, true.B )
        poke(m.io.resp_ready, true.B )
        poke(m.io.cmd_bits_rs1, 0.U )
        poke(m.io.cmd_bits_rs2, 0.U ) // do 4 get_loads: res := mem2(40) (= 30)
        poke(m.io.cmd_bits_inst_funct, get_load.U )
        poke(m.io.cmd_bits_inst_rd, rd.U )

        step(1)
        clock+=1
        //println(clock.toString())

        expect(m.io.resp_valid, true.B )
        expect(m.io.resp_bits_rd, rd.U )
        expect(m.io.resp_bits_data, (30 + x).U )
        expect(m.io.cmd_ready, true.B )


      }
    }
  }


  println("-----------------------------END OF THE TEST-----------------")

}

class TorusTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  // if n < 2, the torus doesn't work because of the loadArbiterClass


  for( n <- 2 until 5){
    for (n_data <- 0 until 10){
      behavior of "LoadTest with " + (n*n) + "PEs and " + n_data + "data for the data exchange"
      it should "Test whether load works correctly, doing store, load and get_load" in {
        chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceTorusTest(n)) {
          c => new LoadTest(c, n*n)
        } should be (true)
      }

      behavior of "ExchangeDataTest with " + (n*n) + "PEs and " + n_data + "data for the data exchange"
      it should "Test whether data exchange works correctly" in {
        chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceTorusTest(n)) {
          c => new ExchangeDataTest(c, n*n, n_data)
        } should be (true)
      }
    }
  }

  

}


