package torus

import chisel3._
import freechips.rocketchip.config.{Config, Parameters}
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.tile._

class TorusTemplate extends Config((site, here, up) => {
    case BuildRoCC => Seq((p: Parameters) => {
            val torus_template = LazyModule.apply(new TorusAccelerator(OpcodeSet.custom0)(p))
            torus_template
        }
    )
  }
)

class TorusTemplateDefaultConfig extends Config(
    new TorusTemplate ++
    new freechips.rocketchip.subsystem.WithNBigCores(1)
)