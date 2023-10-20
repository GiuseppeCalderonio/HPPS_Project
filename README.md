# HPPS_Project

A Chiesel-based implementation of a torus interconnect topology for data movement is pre-
sented in this report.
In particular, instead of using a standard hierarchical memory design that, in some domains can
lead to sub-optimal performances, a bi-dimensional torus topology is proposed, where each matrix
field is a Processing Element(PE), and prototyped via the Chipyard SoC generator as a tightly-
coupled hardware co-processor; the main operation provided by the accelerator concerns data
movement, indeed it has the possibility to exchange data with its neighbour through a command
called ”exchange”.
In this way, each PE can be seen as an independent computing element, but at the same time when-
ever necessary can share data with other PEs, improving the overall data movement bandwidth.
This project will show a toy example of a possible data-movement protocol, thus the implemen-
tation provides a bi-dimensional torus that can load and store, focusing on data exchange. The
accelerator indeed has the possibility to store and load elements inside the memories with a broad-
cast approach, and it allow all the PEs to exchange data blocks to all their ”neighbours”, where a
neighbour of a PE is any other PE physically linked to it through the torus topology. The perfor-
mances of the operation do not depend on the number of elements of the matrix (like a common
load, as reported later in this document), but only on the number of data blocks to exchange.

For further reference, [here](./HTTPS_Report_Torus) is a complete description of the project
