/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.frochr123.fabqr;

import com.frochr123.helper.PreviewImageExport;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.managers.MaterialManager;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.PlfFile;
import com.t_oster.visicut.model.PlfPart;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * FabQRFunctions.java, deal with API of FabQR
 * @author Christian
 */
public class FabQRFunctions
{
  public static final int DEFAULT_FABQR_UPLOAD_TIMEOUT = 15000;

  public static void uploadFabQRProject(String name, String email, String projectName, int licenseIndex, String tools, String description, BufferedImage imageReal, BufferedImage imageScheme, PlfFile plfFile, String lasercutterName, String materialString) throws Exception
  {
    // Check for valid situation, otherwise abort
    if (MainView.getInstance() == null || VisicutModel.getInstance() == null || VisicutModel.getInstance().getPlfFile() == null
        || PreferencesManager.getInstance() == null || PreferencesManager.getInstance().getPreferences() == null
        || !PreferencesManager.getInstance().getPreferences().isFabqrActive() || PreferencesManager.getInstance().getPreferences().getFabqrPrivateURL() == null
        || PreferencesManager.getInstance().getPreferences().getFabqrPrivateURL().isEmpty()
        || MaterialManager.getInstance() == null || MappingManager.getInstance() == null
        || VisicutModel.getInstance().getSelectedLaserDevice() == null)
    {
      throw new Exception("FabQR upload exception: Critical error");
    }

    // Check valid data
    if (name == null || email == null || projectName == null || projectName.length() < 3 || licenseIndex < 0
        || tools == null || tools.isEmpty() || description == null || description.isEmpty()
        || imageScheme == null || plfFile == null
        || lasercutterName == null || lasercutterName.isEmpty() || materialString == null || materialString.isEmpty())
    {
      throw new Exception("FabQR upload exception: Invalid input data");
    }
    
    // Convert images to byte data for PNG, imageReal is allowed to be empty
    byte[] imageSchemeBytes = null;
    ByteArrayOutputStream imageSchemeOutputStream = new ByteArrayOutputStream();
    PreviewImageExport.writePngToOutputStream(imageSchemeOutputStream, imageScheme);
    imageSchemeBytes = imageSchemeOutputStream.toByteArray();

    if (imageSchemeBytes == null)
    {
      throw new Exception("FabQR upload exception: Error converting scheme image");
    }
    
    byte[] imageRealBytes = null;

    if (imageReal != null)
    {
      ByteArrayOutputStream imageRealOutputStream = new ByteArrayOutputStream();
      PreviewImageExport.writePngToOutputStream(imageRealOutputStream, imageReal);
      imageRealBytes = imageRealOutputStream.toByteArray();    
    }

    // Extract all URLs from used QR codes
    List<String> urlList = new LinkedList<String>();
    List<PlfPart> plfParts = plfFile.getPartsCopy();
    
    for (PlfPart plfPart : plfParts)
    {
      if (plfPart.getQRCodeInfo() != null && plfPart.getQRCodeInfo().getQRCodeSourceURL() != null && !plfPart.getQRCodeInfo().getQRCodeSourceURL().trim().isEmpty())
      {
        // Process url, if it is URL of a FabQR system, remove download flag and point to project page instead
        // Use regex to check for FabQR system URL structure
        String qrCodeUrl = plfPart.getQRCodeInfo().getQRCodeSourceURL().trim();

        // Simple and inaccurate check for URL structure of FabQR system
        Pattern fabQRUrlPattern = Pattern.compile("^https{0,1}://.{0,20}/d/([a-z]|[0-9]){7,7}$");

        if (fabQRUrlPattern.matcher(email).find())
        {
          qrCodeUrl = qrCodeUrl.replace("/d/", "/");
        }

        // Add URL if it is not yet in list
        if (!urlList.contains(qrCodeUrl))
        {
          urlList.add(qrCodeUrl);
        }
      }
    }
    
    String usedURLs = "";
    
    for (String url : urlList)
    {
      // Add comma for non first entries
      if (!usedURLs.isEmpty())
      {
        usedURLs = usedURLs + ", ";
      }
      
      usedURLs = usedURLs + url;
    }

    // Get bytes for PLF file
    byte[] plfFileBytes = null;
    ByteArrayOutputStream plfFileOutputStream = new ByteArrayOutputStream();
    VisicutModel.getInstance().savePlfToStream(MaterialManager.getInstance(), MappingManager.getInstance(), plfFileOutputStream);
    plfFileBytes = plfFileOutputStream.toByteArray();
    
    if (plfFileBytes == null)
    {
      throw new Exception("FabQR upload exception: Error saving PLF file");
    }

    // Begin uploading data
    String uploadUrl = PreferencesManager.getInstance().getPreferences().getFabqrPrivateURL();

    // Create HTTP client and cusomized config for timeouts
    CloseableHttpClient httpClient = HttpClients.createDefault();
    RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(DEFAULT_FABQR_UPLOAD_TIMEOUT).setConnectTimeout(DEFAULT_FABQR_UPLOAD_TIMEOUT).setConnectionRequestTimeout(DEFAULT_FABQR_UPLOAD_TIMEOUT).build();

    // Create HTTP Post request and entity builder
    HttpPost httpPost = new HttpPost(uploadUrl);
    httpPost.setConfig(requestConfig);
    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
    
    // Insert file uploads
    multipartEntityBuilder.addBinaryBody("imageScheme", imageSchemeBytes, ContentType.APPLICATION_OCTET_STREAM, "imageScheme.png");
    multipartEntityBuilder.addBinaryBody("plfFile", plfFileBytes, ContentType.APPLICATION_OCTET_STREAM, "plfFile.plf");

    // Image real is allowed to be null, if it is not, send it
    if (imageRealBytes != null)
    {
      multipartEntityBuilder.addBinaryBody("imageReal", plfFileBytes, ContentType.APPLICATION_OCTET_STREAM, "imageReal.png");
    }

    // Insert text data
    multipartEntityBuilder.addTextBody("name", name);
    multipartEntityBuilder.addTextBody("email", email);
    multipartEntityBuilder.addTextBody("licenseIndex", new Integer(licenseIndex).toString());
    multipartEntityBuilder.addTextBody("tools", tools);
    multipartEntityBuilder.addTextBody("description", description);
    multipartEntityBuilder.addTextBody("lasercutterName", lasercutterName);
    multipartEntityBuilder.addTextBody("materialString", materialString);
    multipartEntityBuilder.addTextBody("usedURLs", usedURLs);

    // Assign entity to this post request
    HttpEntity httpEntity = multipartEntityBuilder.build();
    httpPost.setEntity(httpEntity);

    // Set authentication information
    String encodedCredentials = Helper.getEncodedCredentials(VisicutModel.getInstance().getSelectedLaserDevice().getURLUser(), VisicutModel.getInstance().getSelectedLaserDevice().getURLPassword());
    if (!encodedCredentials.isEmpty())
    {
      httpPost.addHeader("Authorization", "Basic " + encodedCredentials);
    }

    // Send request
    CloseableHttpResponse res = httpClient.execute(httpPost);
    res.close();
    httpClient.close();
  }
}
