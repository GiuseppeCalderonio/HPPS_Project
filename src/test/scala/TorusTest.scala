
package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class CustomInterfaceTorusTest () extends Module{
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

    val t = Module(new Torus())


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

class LoadTest(m : CustomInterfaceTorusTest) extends PeekPokeTester(m){

  val load = 0
  val store = 1
  val get_load = 2
  val exchange = 3
  var rd = 7

  // do a store on the 2nd memory mem2(10) := 30

  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 30.U )
  poke(m.io.cmd_bits_rs2, (10 + (2 << 16)).U )
  poke(m.io.cmd_bits_inst_funct, store.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  // do a load in the 2nd memory: data := mem2(10) (= 30)

  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 0.U )
  poke(m.io.cmd_bits_rs2, (10 + (2 << 16)).U )
  poke(m.io.cmd_bits_inst_funct, load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  // do 4 get_load and verify that the values were actually stored and then loaded


  for ( i <- 0 until 4){
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )
    poke(m.io.cmd_bits_rs1, 0.U )
    poke(m.io.cmd_bits_rs2, 0.U )
    poke(m.io.cmd_bits_inst_funct, get_load.U )
    poke(m.io.cmd_bits_inst_rd, rd.U )

    step(1)

    expect(m.io.resp_valid, true.B )
    expect(m.io.resp_bits_rd, rd.U )
    expect(m.io.resp_bits_data, 30.U )
    expect(m.io.cmd_ready, true.B )
  }

  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )
  //poke(m.io.cmd_bits_rs1, 20.U )
  //poke(m.io.cmd_bits_rs2, (20 + (2 << 16)).U )
  poke(m.io.cmd_bits_inst_funct, get_load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)

  expect(m.io.resp_valid, false.B )
  expect(m.io.cmd_ready, true.B )

}

class ExchangeDataTest(m : CustomInterfaceTorusTest) extends PeekPokeTester(m){

  val load = 0
  val store = 1
  val get_load = 2
  val exchange = 3
  var rd = 7

  // do 3 store on the main memory memM(10 + i) := 30 + i

  for(i <- 0 until 3){
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )
    poke(m.io.cmd_bits_rs1, (30 + i).U )
    poke(m.io.cmd_bits_rs2, (10 + i).U )
    poke(m.io.cmd_bits_inst_funct, store.U )
    poke(m.io.cmd_bits_inst_rd, rd.U )

    step(1)

    expect(m.io.resp_valid, true.B )
    expect(m.io.resp_bits_rd, rd.U )
    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_bits_data, 13.U )
  }

  // do a data exchange(src = 10, n = 3, dest = 40)

  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, (10 + (3 << 16)).U )
  poke(m.io.cmd_bits_rs2, 40.U )
  poke(m.io.cmd_bits_inst_funct, exchange.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  for( i <- 0 until 3){
    step(1)

    expect(m.io.resp_valid, true.B )
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

  step(1) // 6 -> 7

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  // do a load: res := mem2(40) (= 30)

  poke(m.io.cmd_valid, true.B ) // this is false, rest is useess
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 0.U )
  poke(m.io.cmd_bits_rs2, (40 + (2 << 16)).U ) // do 3 loads: res := mem2(40 + i) (= 30 + i)
  poke(m.io.cmd_bits_inst_funct, load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  for(i <- 0 until 4){

    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )
    poke(m.io.cmd_bits_rs1, 0.U )
    poke(m.io.cmd_bits_rs2, 0.U ) // do 4 get_loads: res := mem2(40) (= 30)
    poke(m.io.cmd_bits_inst_funct, get_load.U )
    poke(m.io.cmd_bits_inst_rd, rd.U )

    step(1)

    expect(m.io.resp_valid, true.B )
    expect(m.io.resp_bits_rd, rd.U )
    expect(m.io.resp_bits_data, 30.U )
    expect(m.io.cmd_ready, true.B )


  }

  // do a load: res := mem2(41) (= 31)

  poke(m.io.cmd_valid, true.B ) // this is false, rest is useess
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 0.U )
  poke(m.io.cmd_bits_rs2, (41 + (2 << 16)).U ) // do 3 loads: res := mem2(40 + i) (= 30 + i)
  poke(m.io.cmd_bits_inst_funct, load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  for(i <- 0 until 4){
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )
    poke(m.io.cmd_bits_rs1, 0.U )
    poke(m.io.cmd_bits_rs2, 0.U ) // do a get_loads: res := mem2(41) (= 11)
    poke(m.io.cmd_bits_inst_funct, get_load.U )
    poke(m.io.cmd_bits_inst_rd, rd.U )

    step(1)

    expect(m.io.resp_valid, true.B )
    expect(m.io.resp_bits_rd, rd.U )
    expect(m.io.resp_bits_data, 31.U )
    expect(m.io.cmd_ready, true.B )
  }

  // do a load: res := mem2(42) (= 32)

  poke(m.io.cmd_valid, true.B ) // this is false, rest is useess
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 0.U )
  poke(m.io.cmd_bits_rs2, (42 + (2 << 16)).U ) // do 3 loads: res := mem2(42) (= 32)
  poke(m.io.cmd_bits_inst_funct, load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)

  expect(m.io.resp_valid, true.B )
  expect(m.io.resp_bits_rd, rd.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_bits_data, 13.U )

  for(i <- 0 until 4){

    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )
    poke(m.io.cmd_bits_rs1, 0.U )
    poke(m.io.cmd_bits_rs2, 0.U ) // do a get_loads: res := mem2(42) (= 32)
    poke(m.io.cmd_bits_inst_funct, get_load.U )
    poke(m.io.cmd_bits_inst_rd, rd.U )

    step(1)

    expect(m.io.resp_valid, true.B )
    expect(m.io.resp_bits_rd, rd.U )
    expect(m.io.resp_bits_data, 32.U )
    expect(m.io.cmd_ready, true.B )

  }

  poke(m.io.cmd_valid, true.B )  
  poke(m.io.resp_ready, true.B )
  poke(m.io.cmd_bits_rs1, 0.U )
  poke(m.io.cmd_bits_rs2, 0.U ) // do a get_loads: res := mem2(42) (= 32)
  poke(m.io.cmd_bits_inst_funct, get_load.U )
  poke(m.io.cmd_bits_inst_rd, rd.U )

  step(1)

  expect(m.io.resp_valid, false.B )

}

class TorusTest extends ChiselFlatSpec {

  val testerArgs = Array("")

  behavior of "LoadTest"
  it should "Test whether load works correctly, doing store, load and get_load" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceTorusTest()) {
      c => new LoadTest(c)
    } should be (true)
  }

  behavior of "ExchangeDataTest"
  it should "Test whether data exchange works correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfaceTorusTest()) {
      c => new ExchangeDataTest(c)
    } should be (true)
  }

}


