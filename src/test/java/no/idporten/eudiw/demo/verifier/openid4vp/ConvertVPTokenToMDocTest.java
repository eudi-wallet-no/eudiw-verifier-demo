package no.idporten.eudiw.demo.verifier.openid4vp;

import id.walt.mdoc.dataelement.ByteStringElement;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.MapKey;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When convert vp-token of format mDoc to MDoc")
class ConvertVPTokenToMDocTest {

    @DisplayName("then valid parsing to MDoc")
    @Nested
    class SuccessfulConversions {

        @DisplayName("with Hellas is correct with expected claims and verifications pass")
        @Test
        void testMDoc_Hellas_ok() throws Exception {
            String vpToken = "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOBo2dkb2NUeXBld2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xbGlzc3VlclNpZ25lZKJqbmFtZVNwYWNlc6F3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjGF2BhYbKRmcmFuZG9tWCCuITdMuHYbhU2Q2IKXUqfwxD2DVzybPVvdlX9O90lXq2hkaWdlc3RJRANsZWxlbWVudFZhbHVl2QPsajIwMjUtMTItMTFxZWxlbWVudElkZW50aWZpZXJqYmlydGhfZGF0ZdgYWJGkZnJhbmRvbVgg4Q_dCRI14Nc74ldIuAo1RhZJD5XnE4M51-Guh7PZFTJoZGlnZXN0SUQCbGVsZW1lbnRWYWx1ZaNmcmVnaW9uZkF0aGVuc2djb3VudHJ5Z0F0aGVucyBobG9jYWxpdHlmQXRoZW5zcWVsZW1lbnRJZGVudGlmaWVybnBsYWNlX29mX2JpcnRo2BhYZ6RmcmFuZG9tWCAA1KduSMfMnGeSfMe9gIbNJpq9p-n-nOmhdFieffMAIGhkaWdlc3RJRAVsZWxlbWVudFZhbHVlZ01lbmV4ZXNxZWxlbWVudElkZW50aWZpZXJrZmFtaWx5X25hbWXYGFhlpGZyYW5kb21YIDs-ttrcLollC4zW1qhBoIHe6pgO7hbOIPLOQ9JD1DehaGRpZ2VzdElEBmxlbGVtZW50VmFsdWVmTWFyaW9zcWVsZW1lbnRJZGVudGlmaWVyamdpdmVuX25hbWXYGFhjpGZyYW5kb21YIJ2LO3Gml0KFQg0GKqEBw-wBV1fc7iqJSRdoclDeUbd1aGRpZ2VzdElECGxlbGVtZW50VmFsdWWBYk5McWVsZW1lbnRJZGVudGlmaWVya25hdGlvbmFsaXR5amlzc3VlckF1dGiEQ6EBJqEYIVkCjjCCAoowggIwoAMCAQICFBYjAln6qTxKxdVpOl1BFnV5RLKPMAoGCCqGSM49BAMCMD0xHjAcBgNVBAMMFVBJRCBJc3N1ZXIgQ0EgLSBHUiAwMTEOMAwGA1UECgwFR1JORVQxCzAJBgNVBAYTAkdSMB4XDTI1MTEwMzEzMzE1NFoXDTI2MTIwODEzMzE1NFowRzEoMCYGA1UEAwwfc25mLTc0ODY0Lm9rLWtuby5ncm5ldGNsb3VkLm5ldDEOMAwGA1UECgwFR1JORVQxCzAJBgNVBAYTAkdSMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE53qCemrvb7bQBJGVvF14ifZXt1cGxWQDbUTIFAIRxkxsxP7OEVkmDZuBEnVwwFfil5aGqf8rexjQ4EbK8yIF1aOCAQIwgf8wCQYDVR0TBAIwADAdBgNVHQ4EFgQUOU4ffyNvTuxenRZM4hslRcv_B0MweAYDVR0jBHEwb4AUPX1avWYnwa3SFdYC_PVDMRQvh8uhQaQ_MD0xHjAcBgNVBAMMFVBJRCBJc3N1ZXIgQ0EgLSBHUiAwMTEOMAwGA1UECgwFR1JORVQxCzAJBgNVBAYTAkdSghQEeLiLFVj6_ruGfrgQupZz9y4ihDAVBgNVHSUBAf8ECzAJBgcogYxdBQECMDIGA1UdHwQrMCkwJ6AloCOGIWh0dHA6Ly84My4yMTIuNzIuMTE0OjgwODIvY3JsLnBlbTAOBgNVHQ8BAf8EBAMCB4AwCgYIKoZIzj0EAwIDSAAwRQIgGpZ8VJJ314Lu2w5-QAP6MnwYWjKX6b4HmRILDOgoZCICIQCCBp8lPOdUUEagipA_wrLQnIGcFPBgQgmXqN0RpKyEnVkCfNgYWQJ3pmdkb2NUeXBld2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xZ3ZlcnNpb25jMS4wbHZhbGlkaXR5SW5mb6Nmc2lnbmVkwHQyMDI1LTEyLTExVDAwOjAwOjAwWml2YWxpZEZyb23AdDIwMjUtMTItMTFUMDA6MDA6MDBaanZhbGlkVW50aWzAdDIwMjYtMDMtMTFUMDA6MDA6MDBabHZhbHVlRGlnZXN0c6F3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjGpAFggaucPYsROZXW8LGLjM-kcItcr0sd2iWXfHPY53wrNb00BWCAgOqiMKvukHywVEB3K7LMMQE4TrFNQbMp-pFuEAy0WrQJYIKiQvI9R0PFdzFBp02g3OXlU77WEK11NFPuXjhXmYg8xA1ggM0s8zz6eRoNSah1g47AmMxQW3hpo9NzcWGctDwcACvwEWCAv02cEl7-jjhtx7zR6B3cAjnN-nZwy5xi4taK33VV1pgVYIGF-j4fkFZ4VD0O9-VJyhvx_wNPwN1y_XeuZCyc-kEkFBlgg5fQp6qHEUbJaiCDp9tYyVeSP6Vj6qa5RYKbXttSsmN0HWCD80YQ9vTwBB7GZHCnl6I1lkMYDSNh47JpCTg8KrI3_ighYIKtehrkKbNbvKZp7WkIo9PeXVD4yXXlfaGGE1kLl7fyIbWRldmljZUtleUluZm-haWRldmljZUtleaQBAiABIVggtWwb3SUKgdZ7urVYiJYH6JToSClsJVPZhqtBRuh0mnAiWCDuHuYAROh6bjolcStd7aGGbxuhsdiwIsK3eMNGjPSToG9kaWdlc3RBbGdvcml0aG1nU0hBLTI1NlhAKpmV-RrVkM4LuPQX6fAN0E016jMzhAFh1M-TuWg_SYhY8_R03VsmRRosup616k_A_ReEAAUPcJYatSvvrvbxrWxkZXZpY2VTaWduZWSiam5hbWVTcGFjZXPYGEGgamRldmljZUF1dGihb2RldmljZVNpZ25hdHVyZYRDoQEmoPZYQCHV1ZppEq0-1A8i3xhpHX07SlPZ-E0t2Bx0ORQlyTWV1PC3wca4u0LNgToSeUJOcF7gl18PWbsp7yL5PHHNAF9mc3RhdHVzAA";
            DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
            List<MDoc> documents = deviceResponse.getDocuments();
            assertNotNull(documents);
            MDoc mDoc = documents.getFirst();
            assertNotNull(mDoc);
            assertNotNull(mDoc.getIssuerSigned());
            assertTrue(mDoc.verifyValidity());
            assertTrue(mDoc.verifyDocType());
            assertNotNull(mDoc.getMSO());
        }

        @DisplayName("with Norsk PID with only familyName shared is correct with expected claim and verifications pass")
        @Test
        void testMDoc_Norsk_onlyFamilyName_ok() throws Exception {
            String vpToken = "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOBo2dkb2NUeXBld2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xbGlzc3VlclNpZ25lZKJqbmFtZVNwYWNlc6F3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjGB2BhYVqRoZGlnZXN0SUQBZnJhbmRvbVCDbQw7Ao6oYPfE29GN2MrCcWVsZW1lbnRJZGVudGlmaWVya2ZhbWlseV9uYW1lbGVsZW1lbnRWYWx1ZWdMw5hSREFHamlzc3VlckF1dGiEQ6EBJqEYIVkDNzCCAzMwggLZoAMCAQICCBpMCKscrxA7MAoGCCqGSM49BAMEMGcxGDAWBgNVBGETD05UUk5PLTk5MTgyNTgyNzELMAkGA1UEBhMCbm8xDzANBgNVBAsTBkRpZ2RpcjEtMCsGA1UEAxMkZWlkYXMyc2FuZGthc3NlIFBJRCBQcm92aWRlciBDQSB0ZXN0MB4XDTI1MTAxNTExMTMwN1oXDTI2MTAxNTExMTMwN1owYjELMAkGA1UEBhMCTk8xMzAxBgNVBAMMKkRpZ2l0YWxpc2VyaW5nc2RpcmVrdG9yYXRldCAtIFBJRC11dHN0ZWRlcjEeMBwGA1UEYQwVTlRSTk8tTk9GT1IuOTkxODI1ODI3MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEyyBJnVXHU1X0P-mKbD70uLWkkYnm18HIx1hhFqWS36ym98r6MJlDHtkmg2Q_Bwjl470l32yELIBSnjFPPCJDWgzn8jB212H93CWQhSWKO_fYUC-o9rTKII1DJMqD_nZwo4IBVTCCAVEwHwYDVR0jBBgwFoAUV-174iaowzFYv4NGwK9Bb0LIUgwwHQYDVR0OBBYEFFAiEuWv6S1zIx1fmuAFfDZIkR2TMAwGA1UdEwEB_wQCMAAwXAYDVR0fBFUwUzBRoE-gTYZLaHR0cHM6Ly9jYS50ZXN0LmVpZGFzMnNhbmRrYXNzZS5uZXQvdjEvY2VydHMvaW50ZXJtZWRpYXRlcy9waWRfcHJvdmlkZXIuY3JsMGcGCCsGAQUFBwEBBFswWTBXBggrBgEFBQcwAoZLaHR0cHM6Ly9jYS50ZXN0LmVpZGFzMnNhbmRrYXNzZS5uZXQvdjEvY2VydHMvaW50ZXJtZWRpYXRlcy9waWRfcHJvdmlkZXIuY2VyMA4GA1UdDwEB_wQEAwIFoDAqBggrBgEFBQcBAwEB_wQbMBkGBgQAjkYBBgwPaWQtZXRzaS1xY3QtcGlkMAoGCCqGSM49BAMEA0gAMEUCIDzbte0ifhOtvHFQU9SdQgmUBCPOTCBtYaptcp8MkDfOAiEAuu4PprxAyxfQuYHMDfEN_CBCbt28NBIQrwW1KjzkUDxZAs3YGFkCyKZndmVyc2lvbmMxLjBvZGlnZXN0QWxnb3JpdGhtZ1NIQS0yNTZsdmFsdWVEaWdlc3RzoXdldS5ldXJvcGEuZWMuZXVkaS5waWQuMakAWCDsY8eXbyQAWQa0r-YL4Q2GjO9NJupOfe5CKiLJ40mJVQFYIDtv5ZXYlX4k7Abz9aaUQExPl79FkK6UxkduR1NGFaRpAlggl0dWHcZeIKH2qiucwEYsi9p5pqR-pe22MSRzoqCk2egDWCDlD0gXKrbmQDJuU-iTcnIhbJmttWU8qy52GK3O33kXtQRYIAFoNk4wdKC8e73BdH7e_-W4zcmaKJ0JGrePA3p1b_4GBVggwG0KppjG6xQb5hJUNaSv_P9EfE8mSlTQ_RsD1aUyOy0GWCAixRCxWYQHtR35EihNLAPO-fG81ZVJOj7dbJ-9C_kJPAdYIAulhUEkOsCHYytFHv0Di8d9g4HA-4RiCoj5ekHU_YxNCFggd8OSraM0_RmKEtuMd_lrAoaKCetg4b379lYZmHNJj_ZtZGV2aWNlS2V5SW5mb6FpZGV2aWNlS2V5pAECIAEhWCAfLrv_iWZHV4cAcnykFN41p16R_8IShkWg--nDH2N_OCJYIHCUefrdONjQy7PSKGjChaOEC9tcZWMNfXJN3ZCZpzZTZ2RvY1R5cGV3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjFsdmFsaWRpdHlJbmZvpGZzaWduZWTAeB4yMDI1LTEyLTA0VDEyOjE2OjA4Ljg2OTY2ODEzNVppdmFsaWRGcm9twHgeMjAyNS0xMi0wNFQxMjoxNjowOC44Njk2NzgwMzFaanZhbGlkVW50aWzAeB4yMDI2LTEyLTA0VDEyOjE2OjA4Ljg2OTY3OTc3NlpuZXhwZWN0ZWRVcGRhdGXAeB4yMDI2LTEyLTA0VDEyOjE2OjA4Ljg2OTY4MTQwN1pYQHQrsl5uu9NNtsDwDo7jAfWwSTU8233v9dhksnGkBRgaHYgb5Sb3o5Oq3Js4fPnvM-OeuE86TSBJFtjvbsfR2n5sZGV2aWNlU2lnbmVkompuYW1lU3BhY2Vz2BhBoGpkZXZpY2VBdXRooW9kZXZpY2VTaWduYXR1cmWEQ6EBJqD2WEC5yEdefuqF42-Yr6YirYilQqqMT4U4pqnVoXVA8ZI1_-L_EaMdNR9xeuTEKNUhz_l5P-2jqpQ-_7a89qGnkDyTZnN0YXR1cwA";
            DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
            List<MDoc> documents = deviceResponse.getDocuments();
            assertNotNull(documents);
            MDoc mDoc = documents.getFirst();
            assertNotNull(mDoc);
            assertNotNull(mDoc.getIssuerSigned());
            assertTrue(mDoc.verifyValidity());
            assertTrue(mDoc.verifyDocType());
            assertNotNull(mDoc.getMSO());
            Map<MapKey, DataElement> mDocElements = mDoc.getMSO().toMapElement().getValue();
            assertNotNull(mDocElements);
            Optional<MapKey> valueDigestsKey = mDocElements.keySet().stream().filter(mapKey -> "valueDigests".equals(mapKey.getStr())).findFirst();
            assertNotNull(valueDigestsKey.get());
            DataElement valueDigestsElement = mDocElements.get(valueDigestsKey.get());
            assertNotNull(valueDigestsElement);
            assertNotNull(valueDigestsElement.getInternalValue());
            LinkedHashMap pid = (LinkedHashMap) valueDigestsElement.getInternalValue();
            Optional pidKey = pid.keySet().stream().findFirst();
            assertNotNull(pidKey.get());
            MapElement pidMapElements = (MapElement) pid.get(pidKey.get());
            assertNotNull(pidMapElements);
            Map<MapKey, DataElement> pidClaimsMap = pidMapElements.getValue();
            assertNotNull(pidClaimsMap);
            assertTrue(pidClaimsMap.size() == 9);
            boolean allMatch = pidClaimsMap.values().stream().allMatch(v -> v instanceof ByteStringElement); // this fails with Ukraina vp-token
            assertTrue(allMatch);
        }
    }

    @DisplayName("then fails parsing to MDoc")
    @Nested
    class FailedConversions {

        // only shared familiy_name claim, but the ByteStringElement check fails.
        // CBOR identical to the Norsk one, except differnt ordering of elements in the map for field familiy_name
        final static String vpTokenUkraina = "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOBo2dkb2NUeXBld2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xbGlzc3VlclNpZ25lZKJqbmFtZVNwYWNlc6F3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjGB2BhYY6RmcmFuZG9tWCBW-RTMd9_3kmqx_UGb4L2PKD83Ot8Pk4ve6UA6P3BbqGhkaWdlc3RJRAJsZWxlbWVudFZhbHVlY0doaHFlbGVtZW50SWRlbnRpZmllcmtmYW1pbHlfbmFtZWppc3N1ZXJBdXRohEOhASahGCFZAuMwggLfMIIChaADAgECAhR_eWiFOYMwCZL9hf-riGqhHIkHnjAKBggqhkjOPQQDAjBcMR4wHAYDVQQDDBVQSUQgSXNzdWVyIENBIC0gVVQgMDIxLTArBgNVBAoMJEVVREkgV2FsbGV0IFJlZmVyZW5jZSBJbXBsZW1lbnRhdGlvbjELMAkGA1UEBhMCVVQwHhcNMjUwNDEwMTQzNzUyWhcNMjYwNzA0MTQzNzUxWjBSMRQwEgYDVQQDDAtQSUQgRFMgLSAwMTEtMCsGA1UECgwkRVVESSBXYWxsZXQgUmVmZXJlbmNlIEltcGxlbWVudGF0aW9uMQswCQYDVQQGEwJVVDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABLtYABao_N7RSzfPylqPJU9YFGatFsKLlfaz0a-XJtDK3BO6Zxmd6P0GQt8CCWWhfm2_42BZ8N-C3_Tqz7m1XiWjggEtMIIBKTAfBgNVHSMEGDAWgBRix5RHKL0PohYgp5rCSZRE8QHTxzAbBgNVHREEFDASghBpc3N1ZXIuZXVkaXcuZGV2MBYGA1UdJQEB_wQMMAoGCCuBAgIAAAECMEMGA1UdHwQ8MDowOKA2oDSGMmh0dHBzOi8vcHJlcHJvZC5wa2kuZXVkaXcuZGV2L2NybC9waWRfQ0FfVVRfMDIuY3JsMB0GA1UdDgQWBBSqX-inGRCVjLSWVpOg9sMT-bIRwTAOBgNVHQ8BAf8EBAMCB4AwXQYDVR0SBFYwVIZSaHR0cHM6Ly9naXRodWIuY29tL2V1LWRpZ2l0YWwtaWRlbnRpdHktd2FsbGV0L2FyY2hpdGVjdHVyZS1hbmQtcmVmZXJlbmNlLWZyYW1ld29yazAKBggqhkjOPQQDAgNIADBFAiEA0lVIOypPciQZwpZaBJ65uQM52Ln9QT1vUYX9fl9BEVoCIGnm3q0eHxfAWE-y3M4cyim8EP8bCazREBSCZKfqS7waWQOP2BhZA4qnZnN0YXR1c6Jrc3RhdHVzX2xpc3SiY2lkeBkMpmN1cml4amh0dHBzOi8vaXNzdWVyLmV1ZGl3LmRldi90b2tlbl9zdGF0dXNfbGlzdC9GQy9ldS5ldXJvcGEuZWMuZXVkaS5waWQuMS83MmIxMDZmMS01NWY3LTQzZjktODc2My1mODBiNzdjMjJkMmRvaWRlbnRpZmllcl9saXN0omJpZGQzMjM4Y3VyaXhoaHR0cHM6Ly9pc3N1ZXIuZXVkaXcuZGV2L2lkZW50aWZpZXJfbGlzdC9GQy9ldS5ldXJvcGEuZWMuZXVkaS5waWQuMS83MmIxMDZmMS01NWY3LTQzZjktODc2My1mODBiNzdjMjJkMmRnZG9jVHlwZXdldS5ldXJvcGEuZWMuZXVkaS5waWQuMWd2ZXJzaW9uYzEuMGx2YWxpZGl0eUluZm-jZnNpZ25lZMB0MjAyNS0xMi0xMFQwMDowMDowMFppdmFsaWRGcm9twHQyMDI1LTEyLTEwVDAwOjAwOjAwWmp2YWxpZFVudGlswHQyMDI2LTAzLTEwVDAwOjAwOjAwWmx2YWx1ZURpZ2VzdHOhd2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xqQBYIOPZaX_LDNcMIJnXfIfyEPjdzY0KfjRJAKh9-DvckJ9HAVggcmwz4PeiPurrJw63DBSD23q8AyE-L9ClUh2yO9eyZPkCWCB8XHxPvOT-Gip98Od4LBAGAr1NsXHdi8WVa8S-g1fDXQNYIP2FiTJ7eQ3Zbzc-1ec_GOw61KO3utESYfAD90Ms7y8_BFgg7MCIeKvgTQcYOyGF4kirdMwpuRBBIC3D73_y_lsWtBYFWCDb_2-0JQncm9UeiimPk9Q3RDzTmQFRpvFGVf70mwi2nAZYIBHXb38I6z5Cq9sw09fVQ6M2HAXuaBTUYmsbdtIhDXmIB1ggrpIo16ClZeCQCktwwOjGJI3rmtQwBnPXASBZHTnYy-sIWCDcviPSUN9oAN-wxim9KR7xURYQbpyAvxWA3BVUz18c8W1kZXZpY2VLZXlJbmZvoWlkZXZpY2VLZXmkAQIgASFYILyaO6QPRedQiFPHyQGcGDWlYbH9vX_id5BLef-i-nmtIlggAbmtw9Cds4gbQUdWpX8c0kW-Zq1UVto8F5q4K06Jk1RvZGlnZXN0QWxnb3JpdGhtZ1NIQS0yNTZYQKUkFp3f1mHdoJk52XwBoRO8f5qUf2S4JtHPswSAh2RSoYQQoPQ2GFFhAfWsd-7WATaoJFgtz4YVQgN1pwgf-4VsZGV2aWNlU2lnbmVkompuYW1lU3BhY2Vz2BhBoGpkZXZpY2VBdXRooW9kZXZpY2VTaWduYXR1cmWEQ6EBJqD2WEChIw6rO51sXFyv1kwLYliPitXQXY214RfCPMCQN4rnXLf3AgA9eaMK3YtK3LFnQy-KgdZ0UBeC5u8N_GRRVc-TZnN0YXR1cwA";


        @DisplayName("with PID fails to convert to MDoc due to id key IdentifierListInfo expected to be of type byteString, but is textString")
        @ParameterizedTest
        @ValueSource(strings = {vpTokenUkraina})
        void convertToMDocFailsInWaltId(String vpToken) throws Exception {
            DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
            List<MDoc> documents = deviceResponse.getDocuments();
            assertNotNull(documents);
            MDoc mDoc = documents.getFirst();
            assertNotNull(mDoc);
            assertNotNull(mDoc.getIssuerSigned());
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> mDoc.verifyValidity());
            assertEquals("Value of id key of IdentifierListInfo CBOR map expected to be of type byteString, but instead was found to be of type textString", exception.getMessage());
        }
    }


}
