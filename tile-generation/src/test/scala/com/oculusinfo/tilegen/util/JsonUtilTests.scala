package com.oculusinfo.tilegen.util

import org.json.{JSONObject, JSONArray}
import com.oculusinfo.binning.util.JSONUtilitiesTests
import com.oculusinfo.tilegen.pipeline.PipelineOperations
import org.apache.spark.SharedSparkContext
import org.scalatest.FunSuite

class JsonOperationsTest extends FunSuite with SharedSparkContext {

	def toJsonObject(str: String) : JSONObject = {
		try {
			new JSONObject(str)
		} catch {
			case ex: Exception =>
				assert(false, "Problem with test input JSON: " + str)
				null
		}
	}

	test("Test JSON Null Field Stripping") {

		// Null tests
		val nullJson = List(
			"{ \"name\": null }",																															// Single null field
			"{ \"name\": null, \"array\": [ null, null ] }",																	// Array with null elements
			"{ \"name\": null, \"array\": [ { \"nested\": null } ] }",								      	// Array with nested object with null value
			"{ \"name\": null, \"obj\": { \"objField1\": null, \"objField2\": null } }",    	// Nested object with null fields
		  "{ \"name\": null, \"array\": [ [ null, null ] ] }",                   // Nested arrays with null fields
		  "{ \"name\": null, \"array\": [ [ { \"objField1\": null }, { \"objField1\": null } ] ] }", // Nested arrays containing objects with null fields
			"{ \"obj1\": { \"obj2\": { \"array\": [null, null, null] } } }"                   // Two nested objects containing a null array
		)
		nullJson.foreach { str =>
			val sanityCheckStringObj = toJsonObject(str) // This will let us know whether the input is malformed
			assert(PipelineOperations.purgeNullFieldsFromJsonString(str) == "")
		}

		// Tests that have some content and nulls are stripped
		val nonNullJsonInputOutputPair = List(
		  // Basic case - everything is non-null
			( "{ \"name\": \"abc\" }",
				"{ \"name\": \"abc\" }" ),
		  // Array that contains all null elements
			( "{ \"name\": \"abc\", \"array\": [ null, null, null ] }",
				"{ \"name\": \"abc\" }" ),
		  // Array that contains a null element, but also a non-null primitive element
			( "{ \"name\": null, \"array\": [ null, \"123\"] }",
				"{ \"array\": [ null, \"123\"] }"),
			// Array that contains a null element and a non-null nested object with both a null value and a non-null value
			( "{ \"name\": null, \"array\": [ null, { \"nested\": \"1.5\", \"nestedNull\": null } ] }",
				"{ \"array\": [ null, { \"nested\": \"1.5\" } ] }"),
			// Object that contains a nested null object
			( "{ \"name\": null, \"obj\": { \"objField1\": null, \"objField2\": \"42\" } }",
				"{ \"obj\": { \"objField2\": \"42\" } }"),
			// Object that contains two nested objects and a non-null array
			( "{ \"obj1\": { \"obj2\": { \"array\": [\"1.5\", null, null] } } }",
				"{ \"obj1\": { \"obj2\": { \"array\": [\"1.5\", null, null] } } }"),
			// Object that contains nested arrays
			("{ \"array\": [ [null, null, null], [null, null, {\"value\":\"123\"} ] ] }",
			 "{ \"array\": [ [null, null, null], [null, null, {\"value\":\"123\"} ] ] }")
		)

		nonNullJsonInputOutputPair.foreach { inputOutputPair =>
			val sanityCheckStringObj = toJsonObject(inputOutputPair._1)  // This will let us know whether the input is malformed
			val computedResult = PipelineOperations.purgeNullFieldsFromJsonString(inputOutputPair._1)
			JSONUtilitiesTests.assertJsonEqual(toJsonObject(inputOutputPair._2), toJsonObject(computedResult))
		}
	}

}
