package no.idporten.eudiw.demo.verifier.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.demo.verifier.openid4vp.OpenID4VPRequestService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.awt.image.BufferedImage;

@RequiredArgsConstructor
@Controller
public class QrCodeController {

    private final OpenID4VPRequestService openID4VPRequestService;

    @GetMapping(value = "/qrcode/{type}/{verifierTransactionId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<BufferedImage> getQrCodeImage(@PathVariable("type") String type, @PathVariable("verifierTransactionId") String verifierTransactionId) throws Exception {
        return ResponseEntity.ok(generateQRCodeImage(type, verifierTransactionId));
    }

    @ExceptionHandler
    public String handleException(Exception e) {
        System.out.println("Request handling failed: " + e.getMessage());
        return "error";
    }

    private BufferedImage generateQRCodeImage(String type, String verifierTransactionId) throws Exception {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix =
                barcodeWriter.encode(openID4VPRequestService.createAuthorizationRequest(verifierTransactionId, "cross-device").toString(), BarcodeFormat.QR_CODE, 200, 200);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

}
