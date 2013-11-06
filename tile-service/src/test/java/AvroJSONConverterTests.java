import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.io.Pair;
import com.oculusinfo.binning.io.TileSerializer;
import com.oculusinfo.binning.io.impl.StringDoublePairArrayAvroSerializer;
import com.oculusinfo.tile.spi.AvroJSONConverter;


public class AvroJSONConverterTests {
    private static final double EPSILON = 1E-12;
    private static InputStream toInputStream (Schema schema, GenericRecord... records) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
        dataFileWriter.create(schema, stream);
        for (GenericRecord record: records) {
            dataFileWriter.append(record);
        }
        dataFileWriter.close();
        stream.flush();

        return new ByteArrayInputStream(stream.toByteArray());
    }

    @Test
    public void testSimpleRecord () throws IOException, JSONException {
        Schema schema = new Parser().parse("{ \"name\": \"test\", \"type\": \"record\", \"fields\": [ { \"name\": \"value\", \"type\": \"double\" } ] }");
        GenericRecord record = new GenericData.Record(schema);
        record.put("value", 3.4);

        JSONObject result = AvroJSONConverter.convert(toInputStream(schema, record));
        Assert.assertEquals(3.4, result.getDouble("value"), EPSILON);
    }

    @Test
    public void testNestedRecord () throws IOException, JSONException {
        Schema schema = new Parser().parse("{ "
                                           + "\"name\": \"test\", "
                                           + "\"type\": \"record\", "
                                           + "\"fields\": "
                                           +" [ "
                                           + "  { "
                                           + "    \"name\": \"rval\", "
                                           + "    \"type\": {"
                                           + "      \"name\": \"rvalEntry\", "
                                           + "      \"type\": \"record\", "
                                           + "      \"fields\": [ { \"name\": \"a\", \"type\": \"int\" }, "
                                           + "                    { \"name\": \"b\", \"type\": \"string\" } ] "
                                           + "    }"
                                           + "  }, "
                                           + "  { \"name\": \"dval\", \"type\": \"double\" } "
                                           + "] "
                                          +"}");
        GenericRecord record = new GenericData.Record(schema);
        GenericRecord subRecord = new GenericData.Record(schema.getField("rval").schema());
        subRecord.put("a", 2);
        subRecord.put("b", "abc");
        record.put("rval", subRecord);
        record.put("dval", 3.6);

        JSONObject result = AvroJSONConverter.convert(toInputStream(schema, record));
        Assert.assertEquals(3.6, result.getDouble("dval"), EPSILON);
        JSONObject subResult = result.getJSONObject("rval");
        Assert.assertEquals(2, subResult.getInt("a"));
        Assert.assertEquals("abc", subResult.getString("b"));
    }

    @Test
    public void testReadWordScoreTile () throws IOException, JSONException {
        // Create a tile to test
        TilePyramid pyramid = new AOITilePyramid(0, 0, 1, 1);
        TileSerializer<List<Pair<String, Double>>> serializer = new StringDoublePairArrayAvroSerializer();
        TileIndex index = new TileIndex(0, 0, 0, 1, 1);
        TileData<List<Pair<String, Double>>> tile = new TileData<>(index);
        List<Pair<String, Double>> bin = new ArrayList<>();
        bin.add(new Pair<String, Double>("abc", 1.0));
        bin.add(new Pair<String, Double>("def", 1.5));
        bin.add(new Pair<String, Double>("ghi", 2.0));
        bin.add(new Pair<String, Double>("jkl", 2.25));
        tile.setBin(0, 0, bin);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(tile, pyramid, baos);
        baos.flush();
        baos.close();
        byte[] serializedTileData = baos.toByteArray();

        // Now try to convert that to JSON.
        JSONObject result = AvroJSONConverter.convert(new ByteArrayInputStream(serializedTileData));
        System.out.println(result.toString());
    }
}
