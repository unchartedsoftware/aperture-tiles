package com.oculusinfo.tile.spi;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.FileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;



/**
 * Simple utility class to convert Avro files to JSON (and, maybe, vice versa)
 * 
 * @author nkronenfeld
 */
public class AvroJSONConverter {
    /**
     * Convert an Avro input stream into a JSON object
     * 
     * @param stream The input data
     * @return A JSON representation of the input data
     * @throws IOException
     * @throws JSONException
     */
    public static JSONObject convert (InputStream stream) throws IOException, JSONException {
        SeekableInput input = new SeekableByteArrayInput(IOUtils.toByteArray(stream));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Conversion code taken from org.apache.avro.tool.DataFileReadTool
        GenericDatumReader<Object> reader = new GenericDatumReader<Object>();
        FileReader<Object> fileReader = DataFileReader.openReader(input, reader);
        try {
            Schema schema = fileReader.getSchema();
            DatumWriter<Object> writer = new GenericDatumWriter<Object>(schema);
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, output);
            for (Object datum: fileReader) {
                encoder.configure(output);
                writer.write(datum, encoder);
                encoder.flush();
                // For some reason, we only contain one record, but the 
                // decoding thinks we contain more and fails; so just break 
                // after our first one.
                break;
            }
            output.flush();
        } finally {
            fileReader.close();
        }
        String jsonString = output.toString();
        return new JSONObject(jsonString);
    }
}
