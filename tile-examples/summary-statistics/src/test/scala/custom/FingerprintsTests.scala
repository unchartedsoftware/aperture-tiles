package com.oculusinfo.stats.customAnalytics

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import org.scalatest.FunSuite

class FingerprintsTestSuite extends FunSuite {

  //test isHex
  test("isHex - empty string") {
    assert(Fingerprints.isHex("") === false)
  }
  
  //test scoreRange
  
  test("scoreRange 1 dash situations") {
    val a1 = 5.toString
    val a2 = 0.toString
    val a3 = -5.toString
    val a4 = -3.toString
    
    assert(Fingerprints.scoreRange(a1, "-5") === false)
    assert(Fingerprints.scoreRange(a2, "-5") === false)
    assert(Fingerprints.scoreRange(a3, "-5") === true)
    
    assert(Fingerprints.scoreRange(a1, "3-10") === true)
    assert(Fingerprints.scoreRange(a2, "3-10") === true)
    assert(Fingerprints.scoreRange(a3, "3-10") === true)
  
  }
  
  test("scoreRange 2 dash situations"){
    val a1 = 5.toString
    val a2 = 0.toString
    val a3 = -5.toString
    
    assert(Fingerprints.scoreRange(a1, "-3-3") === false)
    assert(Fingerprints.scoreRange(a2, "-3-3") === true)
    assert(Fingerprints.scoreRange(a3, "-3-3") === false)   
  }
  
  test("scoreRange 3 dash situations"){
    val a1 = 5.toString
    val a2 = 0.toString
    val a3 = -5.toString
    
    assert(Fingerprints.scoreRange(a1, "-10--3") === false)
    assert(Fingerprints.scoreRange(a2, "-10--3") === false)
    assert(Fingerprints.scoreRange(a3, "-10--3") === true)
  }
  
  //test scoreGreater
  
  test("scoreGreater tests"){
    val a1 = 5.toString
    val a2 = -1.toString
    val a3 = -5.toString
    
    assert(Fingerprints.scoreRange(a1, ">0") === true)
    assert(Fingerprints.scoreRange(a2, ">0") === false)
    assert(Fingerprints.scoreRange(a3, ">0") === false)
  
  }

  
  
  test("scorer invalid input tests") {
    
    
    
  }
  //test scoreRange
  //test scoreGreater
  //test fingerPrintsParser
  //test scorer
  
  
// Check each bounds test in both directions
//	test("Union - disjoint bordering") {
//		val b1 = new Bounds(1, new Rectangle[Int](1, 1, 1, 1), None)
//		val b2 = new Bounds(1, new Rectangle[Int](1, 1, 2, 2), None)
//
//		assert((b1 union b2).get.level === 1)
//		assert((b1 union b2).get.indexBounds === new Rectangle[Int](1, 1, 1, 2))
//		assert((b2 union b1).get.level === 1)
//		assert((b2 union b1).get.indexBounds === new Rectangle[Int](1, 1, 1, 2))
//	}	
  
}