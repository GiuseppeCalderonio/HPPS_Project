package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._


class CustomInterfacePETests extends Module{

    val io = IO(new Bundle{

        val cmd_bits_funct = Input(Bits(7.W))
        val cmd_bits_rs1 = Input(Bits(32.W))
        val cmd_bits_rs2 = Input(Bits(32.W))
        val cmd_ready = Output(Bool())
        val cmd_valid = Input(Bool())
        val resp_valid = Output(Bool())
        val resp_ready = Input(Bool())
        val resp_bits_data = Output(Bits(32.W))
        val conn_right_in = Input(Bits(32.W))
        val conn_right_out = Output(Bits(32.W))
        val conn_left_in = Input(Bits(32.W))
        val conn_left_out = Output(Bits(32.W))
    })

    val PE = Module(new PE(1))

    // conecting inputs to module
    PE.io.cmd.bits.funct := io.cmd_bits_funct
    PE.io.cmd.bits.rs1 := io.cmd_bits_rs1
    PE.io.cmd.bits.rs2 := io.cmd_bits_rs2
    PE.io.cmd.valid := io.cmd_valid
    PE.io.resp.ready := io.resp_ready

    PE.io.conn.left.in := io.conn_left_in
    PE.io.conn.right.in := io.conn_right_in


    // connecting outputs to module
    io.cmd_ready := PE.io.cmd.ready
    io.resp_valid := PE.io.resp.valid
    io.resp_bits_data := PE.io.resp.bits.data
    
    io.conn_right_out := PE.io.conn.right.out
    io.conn_left_out := PE.io.conn.left.out

}

class PEPipelineTest(m: CustomInterfacePETests) extends PeekPokeTester(m){

  var n = 10

  for(i <- 0 until n){

    // store
    poke(m.io.cmd_bits_rs1, i.U)
    poke(m.io.cmd_bits_rs2, (i+1).U)
    poke(m.io.cmd_bits_funct, 1.U)
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )

    step(1)

    expect(m.io.resp_bits_data, 100.U )
    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, true.B )

    // load
    poke(m.io.cmd_bits_rs1, (i+1).U)
    poke(m.io.cmd_bits_rs2, 15.U)
    poke(m.io.cmd_bits_funct, 0.U)
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, true.B )

    step(1)

    expect(m.io.resp_bits_data, i.U )
    expect(m.io.cmd_ready, true.B )
    expect(m.io.resp_valid, true.B )

  }

    
}

class PEKeepRegisterValueTest(m: CustomInterfacePETests) extends PeekPokeTester(m){

  var n = 10

  // store
  poke(m.io.cmd_bits_rs1, n.U)
  poke(m.io.cmd_bits_rs2, (n+1).U)
  poke(m.io.cmd_bits_funct, 1.U)
  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )

  step(1)

  expect(m.io.resp_bits_data, 100.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_valid, true.B )



  for(i <- 0 until n){

    // store
    poke(m.io.cmd_bits_rs1, i.U)
    poke(m.io.cmd_bits_rs2, (i+1).U)
    poke(m.io.cmd_bits_funct, 1.U)
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, false.B )

    step(1)

    expect(m.io.resp_bits_data, 100.U )
    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, true.B )

    // load
    poke(m.io.cmd_bits_rs1, (i+1).U)
    poke(m.io.cmd_bits_rs2, 15.U)
    poke(m.io.cmd_bits_funct, 0.U)
    poke(m.io.cmd_valid, true.B )
    poke(m.io.resp_ready, false.B )

    step(1)

    expect(m.io.resp_bits_data, 100.U )
    expect(m.io.cmd_ready, false.B )
    expect(m.io.resp_valid, true.B )

  }

  // load
  poke(m.io.cmd_bits_rs1, (n+1).U)
  poke(m.io.cmd_bits_rs2, (n+1).U)
  poke(m.io.cmd_bits_funct, 0.U)
  poke(m.io.cmd_valid, true.B )
  poke(m.io.resp_ready, true.B )

  step(1)

  expect(m.io.resp_bits_data, 100.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_valid, true.B )

  poke(m.io.cmd_valid, false.B )
  poke(m.io.resp_ready, false.B )

  step(1)

  expect(m.io.resp_bits_data, n.U )
  expect(m.io.cmd_ready, false.B )
  expect(m.io.resp_valid, true.B )

  poke(m.io.cmd_valid, false.B )
  poke(m.io.resp_ready, true.B )

  step(1)

  expect(m.io.resp_bits_data, n.U )
  expect(m.io.cmd_ready, true.B )
  expect(m.io.resp_valid, true.B )
  
}


class PETest extends ChiselFlatSpec {

  val testerArgs = Array("")

  behavior of "PEPipelineTest"
  it should "execute pipelined load and stores" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfacePETests()) {
      c => new PEPipelineTest(c)
    } should be (true)
  }

  behavior of "PEKeepRegisterValueTest"
  it should "stall the execution correctly and keep the values correctly" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new CustomInterfacePETests()) {
      c => new PEKeepRegisterValueTest(c)
    } should be (true)
  }

  
  
}