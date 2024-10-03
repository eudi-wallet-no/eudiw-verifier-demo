package no.idporten.wallet.verifier_demo.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import no.idporten.wallet.verifier_demo.config.ConfigProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.awt.image.BufferedImage;

@Controller
public class QrCodeController {


    @Autowired
    private ConfigProvider configProvider;

    @GetMapping(value = "/qrcode/{state}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<BufferedImage> getQrCodeImage(@PathVariable("state") String state) throws Exception {
        return ResponseEntity.ok(generateQRCodeImage(state));
    }

    @ExceptionHandler
    public String handleException(Exception e) {
        System.out.println("Request handling failed: " + e.getMessage());
        return "error";
    }

    private BufferedImage generateQRCodeImage(String state) throws Exception {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix =
                barcodeWriter.encode(getQrcodeText(state), BarcodeFormat.QR_CODE, 200, 200);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
    private String getQrcodeText(String state) {
        return "eudi-openid4vp://" +configProvider.getSiop2ClientId()
                +"?client_id="+configProvider.getSiop2ClientId()
                +"&request_uri="+configProvider.getExternalBaseUrl()+"/req/" + state;
    }
}
