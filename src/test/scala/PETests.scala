package torus

import chisel3._
import chisel3.iotesters._
import chisel3.util._ 
import org.scalatest._



class LoadAndStoreMainMemoryTest(m : PE) extends PeekPokeTester(m){

  // this signals always take precedence over the main command so they have to be false to let the
  // system work correcly

  poke(m.io.conn.left.in.valid, false.B )
  poke(m.io.conn.right.in.valid, false.B )
  poke(m.io.conn.up.in.valid, false.B )
  poke(m.io.conn.down.in.valid, false.B )

  val load = 0
  val store = 1

  val error_result = 50
  val store_result = 100

  // fail the store because the command is not valid

  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.valid, false.B )
  poke(m.io.cmd.bits.funct, store.U )
  poke(m.io.cmd.bits.rs1, 20.U )
  poke(m.io.cmd.bits.rs2, 5.U )

  step(1)

  expect(m.io.resp.valid, false.B )
  expect(m.io.resp.bits.data, error_result.U )

  // succeed with the store since the command is valid, but the response still not

  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, store.U )
  poke(m.io.cmd.bits.rs1, 20.U )
  poke(m.io.cmd.bits.rs2, 5.U )

  step(1)

  expect(m.io.resp.valid, false.B )
  expect(m.io.resp.bits.data, store_result.U )

  // try a load and fail because the command is not valid

  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.valid, false.B )
  poke(m.io.cmd.bits.funct, load.U )
  //poke(m.io.cmd.bits.rs1, 20.U )
  poke(m.io.cmd.bits.rs2, 5.U )

  step(1)

  expect(m.io.resp.valid, false.B )
  expect(m.io.resp.bits.data, error_result.U )

  // try a load and succeed since the command is valid

  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, load.U )
  //poke(m.io.cmd.bits.rs1, 20.U )
  poke(m.io.cmd.bits.rs2, 5.U )

  step(1)

  expect(m.io.resp.valid, true.B )
  expect(m.io.resp.bits.data, 20.U )
  

}

class LoadAndStoreSideMemoryTest(m : PE) extends PeekPokeTester(m){

  // this signals always take precedence over the main command so they have to be false to let the
  // system work correcly

  poke(m.io.conn.left.in.valid, false.B )
  poke(m.io.conn.right.in.valid, false.B )
  poke(m.io.conn.up.in.valid, false.B )
  poke(m.io.conn.down.in.valid, false.B )

  val load = 0
  val store = 1

  val error_result = 50
  val store_result = 100

  val partial_address = 2 

  // fail the store because the command is not valid

  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.valid, false.B )
  poke(m.io.cmd.bits.funct, store.U )
  poke(m.io.cmd.bits.rs1, 20.U )
  poke(m.io.cmd.bits.rs2, (5 + (partial_address << 16)).U )

  step(1)

  expect(m.io.resp.valid, false.B )
  expect(m.io.resp.bits.data, error_result.U )

  // succeed with the store since the command is valid, but the response still not

  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, store.U )
  poke(m.io.cmd.bits.rs1, 20.U )
  poke(m.io.cmd.bits.rs2, (5 + (partial_address << 16)).U )

  step(1)

  expect(m.io.resp.valid, false.B )
  expect(m.io.resp.bits.data, store_result.U )

  // try a load and fail because the command is not valid

  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.valid, false.B )
  poke(m.io.cmd.bits.funct, load.U )
  //poke(m.io.cmd.bits.rs1, 20.U )
  poke(m.io.cmd.bits.rs2, (5 + (partial_address << 16)).U )

  step(1)

  expect(m.io.resp.valid, false.B )
  expect(m.io.resp.bits.data, error_result.U )

  // try a load and succeed since the command is valid

  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, load.U )
  //poke(m.io.cmd.bits.rs1, 20.U )
  poke(m.io.cmd.bits.rs2, (5 + (partial_address << 16)).U )

  step(1)

  expect(m.io.resp.valid, true.B )
  expect(m.io.resp.bits.data, 20.U )

  // do nothing and verify it does not return anything

  poke(m.io.cmd.valid, false.B )
  poke(m.io.resp.ready, true.B )
  poke(m.io.cmd.bits.funct, load.U )

  step(1)

  expect(m.io.resp.valid, false.B )

}

class ExchnageCommandTest(m : PE, n : Int) extends PeekPokeTester(m, n){

  val src = 20
  val dest = 30

  val store = 1
  val store_result = 100

  // this signals always take precedence over the main command so they have to be false to let the
  // system work correcly during the stores

  poke(m.io.conn.left.in.valid, false.B )
  poke(m.io.conn.right.in.valid, false.B )
  poke(m.io.conn.up.in.valid, false.B )
  poke(m.io.conn.down.in.valid, false.B )

  // this signals for this specific test have to be true because otherwise there
  // is a stall risk

  poke(m.io.conn.left.out.ready, true.B )
  poke(m.io.conn.right.out.ready, true.B )
  poke(m.io.conn.up.out.ready, true.B )
  poke(m.io.conn.down.out.ready, true.B )

  // first of all, store significant data in the main memory
  // [src ] =  

  for(i <- 0 until n){

    poke(m.io.resp.ready, true.B )
    poke(m.io.cmd.valid, true.B )
    poke(m.io.cmd.bits.funct, store.U )
    poke(m.io.cmd.bits.rs1, (10 + i).U )
    poke(m.io.cmd.bits.rs2, (src + i).U )

    step(1)

    expect(m.io.resp.valid, false.B )
    expect(m.io.resp.bits.data, store_result.U )


  }

  expect(m.io.conn.up.out.valid, false.B )

  // from now on , let's do the data exchange

  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, 3.U )
  poke(m.io.cmd.bits.rs1, (src + (n << 16)).U )
  poke(m.io.cmd.bits.rs2, dest.U )

/*
  step(1)

  expect(m.io.conn.up.out.valid, true.B )
  expect(m.io.conn.up.out.bits.data, (10))
  expect(m.io.conn.up.out.bits.address, (dest).U )

  poke(m.io.conn.left.in.valid, true.B )
  poke(m.io.conn.right.in.valid, true.B )
  poke(m.io.conn.up.in.valid, true.B )
  poke(m.io.conn.down.in.valid, true.B )
*/
  for( i <- 0 until n){

    step(1)

    expect(m.io.conn.up.out.valid, true.B )
    expect(m.io.conn.up.out.bits.data, (10 + i))
    //expect(m.io.test, (dest + i).U )

    expect(m.io.conn.up.out.bits.address, (dest + i).U )

    
    
    poke(m.io.conn.left.in.valid, true.B )
    poke(m.io.conn.right.in.valid, true.B )
    poke(m.io.conn.up.in.valid, true.B )
    poke(m.io.conn.down.in.valid, true.B )
    
  }

  


}


class ExchnageStallTest(m : PE, n : Int) extends PeekPokeTester(m, n){
  val src = 20
  val dest = 30

  val store = 1
  val store_result = 100

  // this signals always take precedence over the main command so they have to be false to let the
  // system work correcly during the stores

  poke(m.io.conn.left.in.valid, false.B )
  poke(m.io.conn.right.in.valid, false.B )
  poke(m.io.conn.up.in.valid, false.B )
  poke(m.io.conn.down.in.valid, false.B )

  // this signals for this specific test have to be true because otherwise there
  // is a stall risk

  poke(m.io.conn.left.out.ready, true.B )
  poke(m.io.conn.right.out.ready, true.B )
  poke(m.io.conn.up.out.ready, true.B )
  poke(m.io.conn.down.out.ready, true.B )

  // first of all, store significant data in the main memory

  for(i <- 0 until n){

    poke(m.io.resp.ready, true.B )
    poke(m.io.cmd.valid, true.B )
    poke(m.io.cmd.bits.funct, store.U )
    poke(m.io.cmd.bits.rs1, (10 + i).U )
    poke(m.io.cmd.bits.rs2, (src + i).U )

    step(1)

    expect(m.io.resp.valid, false.B )
    expect(m.io.resp.bits.data, store_result.U )


  }

  expect(m.io.conn.up.out.valid, false.B )

  // from now on , let's do the data exchange

  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, 3.U )
  poke(m.io.cmd.bits.rs1, (src + (n << 16)).U )
  poke(m.io.cmd.bits.rs2, dest.U )
/*
  step(1)

  expect(m.io.conn.up.out.valid, true.B )
  expect(m.io.conn.up.out.bits.data, (10))
  expect(m.io.conn.up.out.bits.address, (dest).U )

  expect(m.io.conn.down.out.valid, true.B )
  expect(m.io.conn.down.out.bits.data, (10))
  expect(m.io.conn.down.out.bits.address, (dest).U )

  expect(m.io.conn.left.out.valid, true.B )
  expect(m.io.conn.left.out.bits.data, (10))
  expect(m.io.conn.left.out.bits.address, (dest).U )

  expect(m.io.conn.right.out.valid, true.B )
  expect(m.io.conn.right.out.bits.data, (10))
  expect(m.io.conn.right.out.bits.address, (dest).U )
    
    
  poke(m.io.conn.left.in.valid, true.B )
  poke(m.io.conn.right.in.valid, true.B )
  poke(m.io.conn.up.in.valid, true.B )
  poke(m.io.conn.down.in.valid, true.B )
*/
  for( i <- 0 until n-3){

    step(1)

    expect(m.io.conn.up.out.valid, true.B )
    expect(m.io.conn.up.out.bits.data, (10 + i))
    expect(m.io.conn.up.out.bits.address, (dest + i).U )

    expect(m.io.conn.down.out.valid, true.B )
    expect(m.io.conn.down.out.bits.data, (10 + i))
    expect(m.io.conn.down.out.bits.address, (dest + i).U )

    expect(m.io.conn.left.out.valid, true.B )
    expect(m.io.conn.left.out.bits.data, (10 + i))
    expect(m.io.conn.left.out.bits.address, (dest + i).U )

    expect(m.io.conn.right.out.valid, true.B )
    expect(m.io.conn.right.out.bits.data, (10 + i))
    expect(m.io.conn.right.out.bits.address, (dest + i).U )

    poke(m.io.conn.left.in.valid, true.B )
    poke(m.io.conn.right.in.valid, true.B )
    poke(m.io.conn.up.in.valid, true.B )
    poke(m.io.conn.down.in.valid, true.B )
    
  }

  for(i <- 0 until 2){

    poke(m.io.conn.left.out.ready, false.B )
    poke(m.io.conn.right.out.ready, true.B )
    poke(m.io.conn.up.out.ready, true.B )
    poke(m.io.conn.down.out.ready, true.B )

    step(1)

    expect(m.io.conn.up.out.valid, true.B )
    expect(m.io.conn.up.out.bits.data, (10 + n - 3))
    expect(m.io.conn.up.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.down.out.valid, true.B )
    expect(m.io.conn.down.out.bits.data, (10 + n - 3))
    expect(m.io.conn.down.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.left.out.valid, true.B )
    expect(m.io.conn.left.out.bits.data, (10 + n - 3))
    expect(m.io.conn.left.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.right.out.valid, true.B )
    expect(m.io.conn.right.out.bits.data, (10 + n - 3))
    expect(m.io.conn.right.out.bits.address, (dest + n - 3).U )

  }

  for(i <- 0 until 2){

    poke(m.io.conn.left.out.ready, true.B )
    poke(m.io.conn.right.out.ready, false.B )
    poke(m.io.conn.up.out.ready, true.B )
    poke(m.io.conn.down.out.ready, true.B )

    step(1)

    expect(m.io.conn.up.out.valid, true.B )
    expect(m.io.conn.up.out.bits.data, (10 + n - 3))
    expect(m.io.conn.up.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.down.out.valid, true.B )
    expect(m.io.conn.down.out.bits.data, (10 + n - 3))
    expect(m.io.conn.down.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.left.out.valid, true.B )
    expect(m.io.conn.left.out.bits.data, (10 + n - 3))
    expect(m.io.conn.left.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.right.out.valid, true.B )
    expect(m.io.conn.right.out.bits.data, (10 + n - 3))
    expect(m.io.conn.right.out.bits.address, (dest + n - 3).U )

  }

  for(i <- 0 until 2){

    poke(m.io.conn.left.out.ready, true.B )
    poke(m.io.conn.right.out.ready, true.B )
    poke(m.io.conn.up.out.ready, false.B )
    poke(m.io.conn.down.out.ready, true.B )

    step(1)

    expect(m.io.conn.up.out.valid, true.B )
    expect(m.io.conn.up.out.bits.data, (10 + n - 3))
    expect(m.io.conn.up.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.down.out.valid, true.B )
    expect(m.io.conn.down.out.bits.data, (10 + n - 3))
    expect(m.io.conn.down.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.left.out.valid, true.B )
    expect(m.io.conn.left.out.bits.data, (10 + n - 3))
    expect(m.io.conn.left.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.right.out.valid, true.B )
    expect(m.io.conn.right.out.bits.data, (10 + n - 3))
    expect(m.io.conn.right.out.bits.address, (dest + n - 3).U )

  }

  for(i <- 0 until 2){

    poke(m.io.conn.left.out.ready, true.B )
    poke(m.io.conn.right.out.ready, true.B )
    poke(m.io.conn.up.out.ready, true.B )
    poke(m.io.conn.down.out.ready, false.B )

    step(1)

    expect(m.io.conn.up.out.valid, true.B )
    expect(m.io.conn.up.out.bits.data, (10 + n - 3))
    expect(m.io.conn.up.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.down.out.valid, true.B )
    expect(m.io.conn.down.out.bits.data, (10 + n - 3))
    expect(m.io.conn.down.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.left.out.valid, true.B )
    expect(m.io.conn.left.out.bits.data, (10 + n - 3))
    expect(m.io.conn.left.out.bits.address, (dest + n - 3).U )

    expect(m.io.conn.right.out.valid, true.B )
    expect(m.io.conn.right.out.bits.data, (10 + n - 3))
    expect(m.io.conn.right.out.bits.address, (dest + n - 3).U )

  }

  poke(m.io.conn.left.out.ready, true.B )
  poke(m.io.conn.right.out.ready, true.B )
  poke(m.io.conn.up.out.ready, true.B )
  poke(m.io.conn.down.out.ready, true.B )


  for(i <- (n-3) until n){

    step(1)

    expect(m.io.conn.up.out.valid, true.B )
    expect(m.io.conn.up.out.bits.data, (10 + i))
    expect(m.io.conn.up.out.bits.address, (dest + i).U )

    expect(m.io.conn.down.out.valid, true.B )
    expect(m.io.conn.down.out.bits.data, (10 + i))
    expect(m.io.conn.down.out.bits.address, (dest + i).U )

    expect(m.io.conn.left.out.valid, true.B )
    expect(m.io.conn.left.out.bits.data, (10 + i))
    expect(m.io.conn.left.out.bits.address, (dest + i).U )

    expect(m.io.conn.right.out.valid, true.B )
    expect(m.io.conn.right.out.bits.data, (10 + i))
    expect(m.io.conn.right.out.bits.address, (dest + i).U )

  }
}

class SideMemoryExchangeTest(m : PE) extends PeekPokeTester(m){

  val left_addr = 10
  val right_addr = 20
  val up_addr = 30
  val down_addr = 40

  val left_value = 55
  val right_value = 65
  val up_value = 75
  val down_value = 85

  val up_rel_addr = 1
  val down_rel_addr = 2
  val left_rel_addr = 3
  val right_rel_addr = 4

  poke(m.io.conn.left.in.valid, true.B )
  poke(m.io.conn.left.in.bits.address, left_addr.U )
  poke(m.io.conn.left.in.bits.data, left_value.U )

  poke(m.io.conn.right.in.valid, true.B )
  poke(m.io.conn.right.in.bits.address, right_addr.U )
  poke(m.io.conn.right.in.bits.data, right_value.U )

  poke(m.io.conn.up.in.valid, true.B )
  poke(m.io.conn.up.in.bits.address, up_addr.U )
  poke(m.io.conn.up.in.bits.data, up_value.U )


  poke(m.io.conn.down.in.valid, true.B )
  poke(m.io.conn.down.in.bits.address, down_addr.U )
  poke(m.io.conn.down.in.bits.data, down_value.U )

  // this part should be ignored for this clock cycle

  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, 0.U ) // load
  poke(m.io.cmd.bits.rs2, 20.U) // result = mem(20)

  poke(m.io.resp.ready, true.B )

  step(1)

  expect(m.io.resp.valid, false.B )
  expect(m.io.resp.bits.data, 50.U ) // nop of the memory

  // now let's verify if the store were actually executed

  poke(m.io.conn.left.in.valid, false.B )
  poke(m.io.conn.right.in.valid, false.B )
  poke(m.io.conn.up.in.valid, false.B )
  poke(m.io.conn.down.in.valid, false.B )

  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, 0.U ) // load
  poke(m.io.cmd.bits.rs2, (up_addr + (up_rel_addr << 16)).U) // result = mem(20)

  poke(m.io.resp.ready, true.B )

  step(1)

  expect(m.io.resp.valid, true.B )
  expect(m.io.resp.bits.data, up_value.U )

  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, 0.U ) // load
  poke(m.io.cmd.bits.rs2, (down_addr + (down_rel_addr << 16)).U) // result = mem(20)

  poke(m.io.resp.ready, true.B )

  step(1)

  expect(m.io.resp.valid, true.B )
  expect(m.io.resp.bits.data, down_value.U )

  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, 0.U ) // load
  poke(m.io.cmd.bits.rs2, (left_addr + (left_rel_addr << 16)).U) // result = mem(20)

  poke(m.io.resp.ready, true.B )

  step(1)

  expect(m.io.resp.valid, true.B )
  expect(m.io.resp.bits.data, left_value.U )

  poke(m.io.cmd.valid, true.B )
  poke(m.io.cmd.bits.funct, 0.U ) // load
  poke(m.io.cmd.bits.rs2, (right_addr + (right_rel_addr << 16)).U) // result = mem(20)

  poke(m.io.resp.ready, true.B )

  step(1)

  expect(m.io.resp.valid, true.B )
  expect(m.io.resp.bits.data, right_value.U )

}


class PETest extends ChiselFlatSpec {
/*
  val testerArgs = Array("")

  behavior of "LoadAndStoreMainMemoryTest"
  it should "Load and Store values coorectly from the main memory" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE(0)) {
      c => new LoadAndStoreMainMemoryTest(c)
    } should be (true)
  }  

  behavior of "LoadAndStoreSideMemoryTest"
  it should "Load and Store values coorectly from one of the side memories" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE(0)) {
      c => new LoadAndStoreSideMemoryTest(c)
    } should be (true)
  } 

  val n = 5

  behavior of "ExchnageCommandTest"
  it should "load data from the main memory correctly and send them with 1 CK delay to the neighbours n times" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE(0)) {
      c => new ExchnageCommandTest(c, n)
    } should be (true)
  } 

  behavior of "ExchnageStallTest"
  it should "exchange data with neighbours like ExchnageCommandTest, but stalling for some cycles" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE(0)) {
      c => new ExchnageStallTest(c, n)
    } should be (true)
  } 

  behavior of "SideMemoryExchangeTest"
  it should "store data in the side memories" in {
    chisel3.iotesters.Driver.execute( testerArgs, () => new PE(0)) {
      c => new SideMemoryExchangeTest(c)
    } should be (true)
  } 
  */
}