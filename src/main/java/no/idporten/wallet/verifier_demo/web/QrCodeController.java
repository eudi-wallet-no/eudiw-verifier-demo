package no.idporten.wallet.verifier_demo.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import no.idporten.wallet.verifier_demo.service.OID4VPRequestService;
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

    private final OID4VPRequestService OID4VPRequestService;

    @GetMapping(value = "/qrcode/{type}/{state}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<BufferedImage> getQrCodeImage(@PathVariable("type") String type, @PathVariable("state") String state) throws Exception {
        return ResponseEntity.ok(generateQRCodeImage(type, state));
    }

    @ExceptionHandler
    public String handleException(Exception e) {
        System.out.println("Request handling failed: " + e.getMessage());
        return "error";
    }

    private BufferedImage generateQRCodeImage(String type, String state) throws Exception {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix =
                barcodeWriter.encode(OID4VPRequestService.getAuthorizationRequest(type, state), BarcodeFormat.QR_CODE, 200, 200);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

}
