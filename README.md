# Collatz-fractal

Weird self-similarities and patterns are found when expanding the Collatz conjecture to the complex plane. This repo contains GPU-accelerated code for generating this fractal in an interactive and realtime way. This code requires [Lepton-gl](https://github.com/ranchordo/lepton/tree/lepton-gl) to build correctly, which is used in this case for easy GPU-acceleration.
The resources folder needs to be added to the build path for Lepton to recognize the compute shaders. This option in Eclipse is "Use as source folder".

### The Math Involved
While many have expanded the Collatz conjecture to the complex plane and generated fractals by tracking how those points "blow up" to infinity, looking for movement in the other direction can be interesting too, and is more fitting with the original goal of the Collatz conjecture. After all, the conjecture states that all real integers will eventually decrease to 1 when the iteration is applied. This fractal will check how many iterations are required to get the complex number in question into either a circle about the origin with radius 2 or above a very large number (to prevent overflow problems), although this can be changed quite easily by editing the definitions of the bool `incr` within the compute shaders.

These extensions to the complex or real numbers involve eliminating the "branches" in the iteration process using something like modulo 2 to determine whether a number is even or odd in a "branchless" fasion. An iteration is then computed as:

<img src="https://raw.githubusercontent.com/ranchordo/Collatz-fractal/main/svgs/a860a991c97e81e0b23dfd28cfb690cb.svg" align=middle width=173.31903764999998pt height=47.6716218pt/>

Instead of using the mod function for this purpose, as it is discontinuous, we can use any function that is 0 for even integers and 1 for odd integers without disrupting the original Collatz conjecture under this branchless iteration expression. For this reason, we use the function:

<img src="https://raw.githubusercontent.com/ranchordo/Collatz-fractal/main/svgs/f8a21403d0cbd95f4a1d1b1b4e4b24d6.svg" align=middle width=60.260045999999996pt height=33.20539859999999pt/>

Which results in a nice sine wave alternating between 0 and 1. With this, our full iteration expression becomes:

<img src="https://raw.githubusercontent.com/ranchordo/Collatz-fractal/main/svgs/4ebf0e3754aa9a0d5f4a641cceb180b1.svg" align=middle width=162.21425549999998pt height=47.6716218pt/>
