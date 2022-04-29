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


/*

class Port(width: Int) extends Bundle{
    val in = Input(Bits(width.W))
    val out = Output(Bits(width.W))
}

class Connections(width: Int) extends Bundle{
    val up = new Port(width)
    val down = new Port(width)
    val left = new Port(width)
    val right = new Port(width)
    val ingoing = new Port(width)
    val outgoing = new Port(width)
}

class MatrixElementInterface(width: Int) extends Bundle{
    val c = new Connections(width) // new controllerCmd
    val cmd = Flipped(Decoupled(new RoCCCommandCustom)) // inputs : bits, valid (out : ready)
    val resp = Decoupled(new RoCCResponseCustom) // inputs : ready (out : bits, valid)
}

class MatrixElement(width: Int = 32) extends Module{

    val io = IO(new MatrixElementInterface(width))

    val customOpcode = "b0001011".U

    // "b0001011".U = mycustomop

    // funct = 0.U -> up
    // funct = 1.U -> down
    // funct = 2.U -> left
    // funct = 3.U -> right
    // funct = 4.U -> ingoing
    // funct = 5.U -> outgoing
    // funct = 6.U -> store immediate

    val opcode = io.cmd.bits.inst.opcode
    val funct = io.cmd.bits.inst.funct
    val rd = io.cmd.bits.inst.rd
    val rs1 = io.cmd.bits.inst.rs1
    val rs2 = io.cmd.bits.inst.rs2


    val memory = Mem(32, Bits(width.W)) // memory with 32 values that are long width bits
    val lastOpCode = Reg(Bits(7.W))
    val lastFunct = Reg(Bits(7.W))
    val lastDestination = Reg(Bits(5.W))

    lastOpCode := opcode
    lastFunct := funct
    lastDestination := rd

    val isUp = funct === 0.U
    val isDown = funct === 1.U
    val isLeft = funct === 2.U
    val isRight = funct === 3.U
    val isIngoing = funct === 4.U
    val isOutgoing = funct === 5.U
    val isStore = funct === 6.U

    val last_isUp = lastOpCode === 0.U
    val last_isDown = lastOpCode === 1.U
    val last_isLeft = lastOpCode === 2.U
    val last_isRight = lastOpCode === 3.U
    val last_isIngoing = lastOpCode === 4.U
    val last_isOutgoing = lastOpCode === 5.U


    when(opcode === customOpcode){
        when(isUp){ // move data up
            io.c.up.out := memory(rs1)
        }.elsewhen(isDown){ // move data down
            io.c.down.out := memory(rs1)

        }.elsewhen(isLeft){ // move data left
            io.c.left.out := memory(rs1)

        }.elsewhen(isRight){ // move data right
            io.c.right.out := memory(rs1)

        }.elsewhen(isIngoing){ // move data ingoing
            io.c.ingoing.out := memory(rs1)

        }.elsewhen(isOutgoing){ // move data outgoing
            io.c.outgoing.out := memory(rs1)

        }.elsewhen(isStore){ // store data in memory
            memory(rd) := rs1
        }
        when(lastOpCode === customOpcode){
            when(last_isUp){ // store data from down in memory
                memory(lastDestination) := io.c.down.in
            }.elsewhen(last_isDown){ // store data from up in memory
                memory(lastDestination) := io.c.up.in
            }.elsewhen(last_isLeft){ // store data from right in memory
                memory(lastDestination) := io.c.right.in
            }.elsewhen(last_isRight){ // store data from left in memory
                memory(lastDestination) := io.c.left.in
            }.elsewhen(last_isIngoing){ // store data from outgoing in memory
                memory(lastDestination) := io.c.outgoing.in
            }.elsewhen(last_isOutgoing){ // store data from ingoing in memory
                memory(lastDestination) := io.c.ingoing.in
            }
        }

        
    }

}

*/