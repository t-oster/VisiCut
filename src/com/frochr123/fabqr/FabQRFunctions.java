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
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
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
  public static final int FABQR_UPLOAD_TIMEOUT = 15000;
  public static final String FABQR_API_UPLOAD_PROJECT = "api_uploadproject.php";
  public static final String FABQR_DOWNLOAD_MARKER = "d";
  public static final String FABQR_TEMPORARY_MARKER = "t";

  public static String getFabqrPrivateURL()
  {
    if (PreferencesManager.getInstance() != null
        && PreferencesManager.getInstance().getPreferences() != null
        && PreferencesManager.getInstance().getPreferences().getFabqrPrivateURL() != null)
    {
      return PreferencesManager.getInstance().getPreferences().getFabqrPrivateURL();
    }
    
    return "";
  }
  
  public static String getFabqrPublicURL()
  {
    if (PreferencesManager.getInstance() != null
        && PreferencesManager.getInstance().getPreferences() != null
        && PreferencesManager.getInstance().getPreferences().getFabqrPublicURL() != null)
    {
      return PreferencesManager.getInstance().getPreferences().getFabqrPublicURL();
    }
    
    return "";
  }
  
  public static boolean isFabqrActive()
  {
    if (PreferencesManager.getInstance() != null && PreferencesManager.getInstance().getPreferences() != null)
    {
      return PreferencesManager.getInstance().getPreferences().isFabqrActive();
    }

    return false;
  }
  
  public static String getFabqrPrivateUser()
  {
    if (PreferencesManager.getInstance() != null && PreferencesManager.getInstance().getPreferences() != null)
    {
      return PreferencesManager.getInstance().getPreferences().getFabqrPrivateUser();
    }
    
    return "";
  }
  
  public static String getFabqrPrivatePassword()
  {
    if (PreferencesManager.getInstance() != null && PreferencesManager.getInstance().getPreferences() != null)
    {
      return PreferencesManager.getInstance().getPreferences().getFabqrPrivatePassword();
    }
    
    return "";
  }
  
  public static void uploadFabQRProject(String name, String email, String projectName, int licenseIndex, String tools, String description, String location, BufferedImage imageReal, BufferedImage imageScheme, PlfFile plfFile, String lasercutterName, String lasercutterMaterial) throws Exception
  {
    // Check for valid situation, otherwise abort
    if (MainView.getInstance() == null || VisicutModel.getInstance() == null || VisicutModel.getInstance().getPlfFile() == null
        || !isFabqrActive() || getFabqrPrivateURL() == null || getFabqrPrivateURL().isEmpty()
        || MaterialManager.getInstance() == null || MappingManager.getInstance() == null
        || VisicutModel.getInstance().getSelectedLaserDevice() == null)
    {
      throw new Exception("FabQR upload exception: Critical error");
    }

    // Check valid data
    if (name == null || email == null || projectName == null || projectName.length() < 3 || licenseIndex < 0
        || tools == null || tools.isEmpty() || description == null || description.isEmpty() || location == null || location.isEmpty()
        || imageScheme == null || plfFile == null
        || lasercutterName == null || lasercutterName.isEmpty() || lasercutterMaterial == null || lasercutterMaterial.isEmpty())
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
      // Need to convert image, ImageIO.write messes up the color space of the original input image
      BufferedImage convertedImage = new BufferedImage(imageReal.getWidth(), imageReal.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
      ColorConvertOp op = new ColorConvertOp(null);
      op.filter(imageReal, convertedImage);

      ByteArrayOutputStream imageRealOutputStream = new ByteArrayOutputStream();
      ImageIO.write(convertedImage, "jpg", imageRealOutputStream);
      imageRealBytes = imageRealOutputStream.toByteArray();    
    }

    // Extract all URLs from used QR codes
    List<String> referencesList = new LinkedList<String>();
    List<PlfPart> plfParts = plfFile.getPartsCopy();
    
    for (PlfPart plfPart : plfParts)
    {
      if (plfPart.getQRCodeInfo() != null && plfPart.getQRCodeInfo().getQRCodeSourceURL() != null && !plfPart.getQRCodeInfo().getQRCodeSourceURL().trim().isEmpty())
      {
        // Process url, if it is URL of a FabQR system, remove download flag and point to project page instead
        // Use regex to check for FabQR system URL structure
        String qrCodeUrl = plfPart.getQRCodeInfo().getQRCodeSourceURL().trim();

        // Check for temporary URL structure of FabQR system
        Pattern fabQRUrlTemporaryPattern = Pattern.compile("^https{0,1}://.*?" + "/" + FABQR_TEMPORARY_MARKER + "/" + "([a-z]|[0-9]){7,7}$");

        // Do not include link if it is just temporary
        if (fabQRUrlTemporaryPattern.matcher(qrCodeUrl).find())
        {
          continue;
        }
        
        // Check for download URL structure of FabQR system
        // Change URL to point to project page instead
        Pattern fabQRUrlDownloadPattern = Pattern.compile("^https{0,1}://.*?" + "/" + FABQR_DOWNLOAD_MARKER + "/" + "([a-z]|[0-9]){7,7}$");

        if (fabQRUrlDownloadPattern.matcher(qrCodeUrl).find())
        {
          qrCodeUrl = qrCodeUrl.replace("/" + FABQR_DOWNLOAD_MARKER + "/", "/");
        }

        // Add URL if it is not yet in list
        if (!referencesList.contains(qrCodeUrl))
        {
          referencesList.add(qrCodeUrl);
        }
      }
    }
    
    String references = "";
    
    for (String ref : referencesList)
    {
      // Add comma for non first entries
      if (!references.isEmpty())
      {
        references = references + ",";
      }
      
      references = references + ref;
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
    String uploadUrl = getFabqrPrivateURL() + FABQR_API_UPLOAD_PROJECT;

    // Create HTTP client and cusomized config for timeouts
    CloseableHttpClient httpClient = HttpClients.createDefault();
    RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(FABQR_UPLOAD_TIMEOUT).setConnectTimeout(FABQR_UPLOAD_TIMEOUT).setConnectionRequestTimeout(FABQR_UPLOAD_TIMEOUT).build();

    // Create HTTP Post request and entity builder
    HttpPost httpPost = new HttpPost(uploadUrl);
    httpPost.setConfig(requestConfig);
    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
    
    // Insert file uploads
    multipartEntityBuilder.addBinaryBody("imageScheme", imageSchemeBytes, ContentType.APPLICATION_OCTET_STREAM, "imageScheme.png");
    multipartEntityBuilder.addBinaryBody("inputFile", plfFileBytes, ContentType.APPLICATION_OCTET_STREAM, "inputFile.plf");

    // Image real is allowed to be null, if it is not, send it
    if (imageRealBytes != null)
    {
      multipartEntityBuilder.addBinaryBody("imageReal", imageRealBytes, ContentType.APPLICATION_OCTET_STREAM, "imageReal.png");
    }

    // Prepare content type for text data, especially needed for correct UTF8 encoding
    ContentType contentType = ContentType.create("text/plain", Consts.UTF_8);
    
    // Insert text data
    multipartEntityBuilder.addTextBody("name", name, contentType);
    multipartEntityBuilder.addTextBody("email", email, contentType);
    multipartEntityBuilder.addTextBody("projectName", projectName, contentType);
    multipartEntityBuilder.addTextBody("licenseIndex", new Integer(licenseIndex).toString(), contentType);
    multipartEntityBuilder.addTextBody("tools", tools, contentType);
    multipartEntityBuilder.addTextBody("description", description, contentType);
    multipartEntityBuilder.addTextBody("location", location, contentType);
    multipartEntityBuilder.addTextBody("lasercutterName", lasercutterName, contentType);
    multipartEntityBuilder.addTextBody("lasercutterMaterial", lasercutterMaterial, contentType);
    multipartEntityBuilder.addTextBody("references", references, contentType);

    // Assign entity to this post request
    HttpEntity httpEntity = multipartEntityBuilder.build();
    httpPost.setEntity(httpEntity);

    // Set authentication information
    String encodedCredentials = Helper.getEncodedCredentials(FabQRFunctions.getFabqrPrivateUser(), FabQRFunctions.getFabqrPrivatePassword());
    if (!encodedCredentials.isEmpty())
    {
      httpPost.addHeader("Authorization", "Basic " + encodedCredentials);
    }

    // Send request
    CloseableHttpResponse res = httpClient.execute(httpPost);

    // React to possible server side errors
    if (res.getStatusLine() == null || res.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
    {
      throw new Exception("FabQR upload exception: Server sent wrong HTTP status code: " + new Integer(res.getStatusLine().getStatusCode()).toString());
    }

    // Close everything correctly
    res.close();
    httpClient.close();
  }
}
