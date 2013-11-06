package com.oculusinfo.tile.rest;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

import com.oculusinfo.tile.rest.ImageTileResource.ExtensionType;
import com.oculusinfo.tile.rest.ImageTileResource.ResponseType;

public class ImageTileResourceTests {
	@Test
	public void testExtensionTypes () {
		ExtensionType ext;

		ext = ExtensionType.valueOf("jpg");
		Assert.assertEquals(ResponseType.Image, ext.getResponseType());
		Assert.assertEquals(MediaType.IMAGE_JPEG, ext.getMediaType());

		ext = ExtensionType.valueOf("jpeg");
		Assert.assertEquals(ResponseType.Image, ext.getResponseType());
		Assert.assertEquals(MediaType.IMAGE_JPEG, ext.getMediaType());

		ext = ExtensionType.valueOf("png");
		Assert.assertEquals(ResponseType.Image, ext.getResponseType());
		Assert.assertEquals(MediaType.IMAGE_PNG, ext.getMediaType());

		ext = ExtensionType.valueOf("json");
		Assert.assertEquals(ResponseType.Tile, ext.getResponseType());
		Assert.assertEquals(MediaType.APPLICATION_JSON, ext.getMediaType());
	}
}
