
package email;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =========================================================
    // SEND EMPLOYEE QR CODE BY EMAIL
    // =========================================================

    public void sendEmployeeQRCode(
            String employeeEmail,
            String employeeName,
            String employeeCode)
            throws MessagingException, WriterException, IOException {

        // Generate QR Code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(
                employeeCode,
                BarcodeFormat.QR_CODE,
                300,
                300
        );

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        MatrixToImageWriter.writeToStream(
                bitMatrix,
                "PNG",
                outputStream
        );

        byte[] qrCodeBytes = outputStream.toByteArray();

        // Create Email
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, true);

        // Sender and Receiver
        helper.setFrom("smartattendancesystemproject@gmail.com");
        helper.setTo(employeeEmail);

        // Email Subject
        helper.setSubject(
                "Your Attendance QR Code - " + employeeCode
        );

        // Professional HTML Email
        String emailBody =
                "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "</head>"

                + "<body style='"
                + "margin:0;"
                + "padding:0;"
                + "background-color:#f4f6f9;"
                + "font-family:Arial,Helvetica,sans-serif;"
                + "'>"

                + "<div style='"
                + "max-width:650px;"
                + "margin:30px auto;"
                + "background:#ffffff;"
                + "border-radius:12px;"
                + "overflow:hidden;"
                + "box-shadow:0 4px 15px rgba(0,0,0,0.10);"
                + "'>"

                // Header
                + "<div style='"
                + "background:#1f2937;"
                + "padding:25px;"
                + "text-align:center;"
                + "color:white;"
                + "'>"

                + "<h1 style='"
                + "margin:0;"
                + "font-size:24px;"
                + "'>"
                + "Smart Attendance & Payroll System"
                + "</h1>"

                + "<p style='"
                + "margin:8px 0 0;"
                + "font-size:14px;"
                + "color:#d1d5db;"
                + "'>"
                + "Employee Attendance Management"
                + "</p>"

                + "</div>"

                // Main Content
                + "<div style='padding:30px;'>"

                + "<h2 style='"
                + "color:#1f2937;"
                + "margin-top:0;"
                + "'>"
                + "Hello " + employeeName + ","
                + "</h2>"

                + "<p style='"
                + "color:#4b5563;"
                + "font-size:15px;"
                + "line-height:1.6;"
                + "'>"
                + "Your personal attendance QR code has been "
                + "generated successfully."
                + "</p>"

                // Employee Information
                + "<div style='"
                + "background:#f9fafb;"
                + "border:1px solid #e5e7eb;"
                + "border-radius:8px;"
                + "padding:18px;"
                + "margin:20px 0;"
                + "'>"

                + "<p style='margin:6px 0;'>"
                + "<strong>Employee Name:</strong> "
                + employeeName
                + "</p>"

                + "<p style='margin:6px 0;'>"
                + "<strong>Employee Code:</strong> "
                + employeeCode
                + "</p>"

                + "</div>"

                // QR Code
                + "<div style='"
                + "text-align:center;"
                + "margin:25px 0;"
                + "'>"

                + "<h3 style='color:#1f2937;'>"
                + "Your Attendance QR Code"
                + "</h3>"

                + "<img src='cid:employeeQRCode' "
                + "alt='Employee QR Code' "
                + "style='"
                + "width:250px;"
                + "height:250px;"
                + "border:1px solid #e5e7eb;"
                + "padding:10px;"
                + "border-radius:8px;"
                + "'>"

                + "<p style='"
                + "color:#2563eb;"
                + "font-size:18px;"
                + "font-weight:bold;"
                + "'>"
                + employeeCode
                + "</p>"

                + "</div>"

                // Instructions
                + "<div style='"
                + "background:#eff6ff;"
                + "border-left:4px solid #2563eb;"
                + "padding:15px;"
                + "margin-top:25px;"
                + "'>"

                + "<p style='"
                + "margin:0;"
                + "color:#374151;"
                + "font-size:14px;"
                + "line-height:1.6;"
                + "'>"

                + "<strong>How to use your QR code:</strong>"
                + "<br><br>"

                + "1. Keep this QR code safe."
                + "<br>"

                + "2. Open the QR Attendance page."
                + "<br>"

                + "3. Scan your QR code using the camera "
                + "or upload the QR image."
                + "<br>"

                + "4. Complete the OTP verification."
                + "<br>"

                + "5. Scan again later for Check-out."

                + "</p>"

                + "</div>"

                + "<p style='"
                + "color:#4b5563;"
                + "font-size:14px;"
                + "line-height:1.6;"
                + "margin-top:25px;"
                + "'>"

                + "Please do not share your personal QR code "
                + "with other employees."

                + "</p>"

                + "<p style='"
                + "color:#4b5563;"
                + "font-size:14px;"
                + "'>"

                + "Thank you,<br>"
                + "<strong>Smart Attendance & Payroll System</strong>"

                + "</p>"

                + "</div>"

                // Footer
                + "<div style='"
                + "background:#f9fafb;"
                + "border-top:1px solid #e5e7eb;"
                + "padding:18px;"
                + "text-align:center;"
                + "color:#6b7280;"
                + "font-size:12px;"
                + "'>"

                + "This is an automated email. "
                + "Please do not reply to this message."

                + "<br><br>"

                + "Smart Attendance & Payroll System © 2026"

                + "</div>"

                + "</div>"
                + "</body>"
                + "</html>";

        // Set HTML email
        helper.setText(
                emailBody,
                true
        );

        // Display QR inside email
        helper.addInline(
                "employeeQRCode",
                () -> new ByteArrayInputStream(qrCodeBytes),
                "image/png"
        );

        // Attach QR PNG
        helper.addAttachment(
                "QR_" + employeeCode + ".png",
                () -> new ByteArrayInputStream(qrCodeBytes)
        );

        // Send Email
        mailSender.send(message);
    }


    // =========================================================
    // SEND EMPLOYEE LOGIN CREDENTIALS BY EMAIL
    // =========================================================

    public void sendEmployeeLoginCredentials(
            String employeeEmail,
            String employeeName,
            String username,
            String temporaryPassword) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true);

            // Sender and receiver
            helper.setFrom(
                    "smartattendancesystemproject@gmail.com"
            );

            helper.setTo(employeeEmail);

            // Email subject
            helper.setSubject(
                    "Your Smart Attendance & Payroll System Login Details"
            );

            // Professional HTML Email
            String emailBody =
                    "<!DOCTYPE html>"
                    + "<html>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "</head>"

                    + "<body style='"
                    + "margin:0;"
                    + "padding:0;"
                    + "background-color:#f4f6f9;"
                    + "font-family:Arial,Helvetica,sans-serif;"
                    + "'>"

                    + "<div style='"
                    + "max-width:650px;"
                    + "margin:30px auto;"
                    + "background:#ffffff;"
                    + "border-radius:12px;"
                    + "overflow:hidden;"
                    + "box-shadow:0 4px 15px rgba(0,0,0,0.10);"
                    + "'>"

                    // Header
                    + "<div style='"
                    + "background:#1f2937;"
                    + "padding:25px;"
                    + "text-align:center;"
                    + "color:white;"
                    + "'>"

                    + "<h1 style='"
                    + "margin:0;"
                    + "font-size:24px;"
                    + "'>"
                    + "Smart Attendance & Payroll System"
                    + "</h1>"

                    + "<p style='"
                    + "margin:8px 0 0;"
                    + "font-size:14px;"
                    + "color:#d1d5db;"
                    + "'>"
                    + "Employee Login Account"
                    + "</p>"

                    + "</div>"

                    // Main Content
                    + "<div style='padding:30px;'>"

                    + "<h2 style='"
                    + "color:#1f2937;"
                    + "margin-top:0;"
                    + "'>"
                    + "Hello " + employeeName + ","
                    + "</h2>"

                    + "<p style='"
                    + "color:#4b5563;"
                    + "font-size:15px;"
                    + "line-height:1.6;"
                    + "'>"
                    + "Your employee login account has been "
                    + "created successfully."
                    + "</p>"

                    // Login Details
                    + "<div style='"
                    + "background:#f9fafb;"
                    + "border:1px solid #e5e7eb;"
                    + "border-radius:8px;"
                    + "padding:20px;"
                    + "margin:20px 0;"
                    + "'>"

                    + "<p style='margin:8px 0;'>"
                    + "<strong>Username:</strong> "
                    + username
                    + "</p>"

                    + "<p style='margin:8px 0;'>"
                    + "<strong>Temporary Password:</strong> "
                    + temporaryPassword
                    + "</p>"

                    + "</div>"

                    // Login Instructions
                    + "<div style='"
                    + "background:#eff6ff;"
                    + "border-left:4px solid #2563eb;"
                    + "padding:15px;"
                    + "margin-top:20px;"
                    + "'>"

                    + "<p style='"
                    + "margin:0;"
                    + "color:#374151;"
                    + "font-size:14px;"
                    + "line-height:1.6;"
                    + "'>"

                    + "<strong>Login URL:</strong>"
                    + "<br>"
                    + "http://localhost:8080/login"
                    + "<br><br>"

                    + "Please keep your login details confidential."
                    + "<br>"
                    + "You should change your password after "
                    + "your first login."

                    + "</p>"

                    + "</div>"

                    + "<p style='"
                    + "color:#4b5563;"
                    + "font-size:14px;"
                    + "line-height:1.6;"
                    + "margin-top:25px;"
                    + "'>"

                    + "Thank you,<br>"
                    + "<strong>Smart Attendance & Payroll System</strong>"

                    + "</p>"

                    + "</div>"

                    // Footer
                    + "<div style='"
                    + "background:#f9fafb;"
                    + "border-top:1px solid #e5e7eb;"
                    + "padding:18px;"
                    + "text-align:center;"
                    + "color:#6b7280;"
                    + "font-size:12px;"
                    + "'>"

                    + "This is an automated email. "
                    + "Please do not reply to this message."

                    + "<br><br>"

                    + "Smart Attendance & Payroll System © 2026"

                    + "</div>"

                    + "</div>"
                    + "</body>"
                    + "</html>";

            // Set HTML content
            helper.setText(
                    emailBody,
                    true
            );

            // Send Email
            mailSender.send(message);

        } catch (MessagingException e) {

            throw new RuntimeException(
                    "Failed to send employee login credentials email.",
                    e
            );
        }
    }


    // =========================================================
    // SEND ATTENDANCE OTP BY EMAIL
    // =========================================================

    public void sendAttendanceOtp(
            String employeeEmail,
            String employeeName,
            String otpCode) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true);

            // Sender and receiver
            helper.setFrom(
                    "smartattendancesystemproject@gmail.com"
            );

            helper.setTo(employeeEmail);

            // Email subject
            helper.setSubject(
                    "Your Attendance Verification OTP"
            );

            // Professional HTML Email
            String emailBody =
                    "<!DOCTYPE html>"
                    + "<html>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "</head>"

                    + "<body style='"
                    + "margin:0;"
                    + "padding:0;"
                    + "background-color:#f4f6f9;"
                    + "font-family:Arial,Helvetica,sans-serif;"
                    + "'>"

                    + "<div style='"
                    + "max-width:650px;"
                    + "margin:30px auto;"
                    + "background:#ffffff;"
                    + "border-radius:12px;"
                    + "overflow:hidden;"
                    + "box-shadow:0 4px 15px rgba(0,0,0,0.10);"
                    + "'>"

                    // Header
                    + "<div style='"
                    + "background:#1f2937;"
                    + "padding:25px;"
                    + "text-align:center;"
                    + "color:white;"
                    + "'>"

                    + "<h1 style='"
                    + "margin:0;"
                    + "font-size:24px;"
                    + "'>"
                    + "Smart Attendance & Payroll System"
                    + "</h1>"

                    + "<p style='"
                    + "margin:8px 0 0;"
                    + "font-size:14px;"
                    + "color:#d1d5db;"
                    + "'>"
                    + "Secure Attendance Verification"
                    + "</p>"

                    + "</div>"

                    // Main Content
                    + "<div style='padding:30px;'>"

                    + "<h2 style='"
                    + "color:#1f2937;"
                    + "margin-top:0;"
                    + "'>"
                    + "Hello " + employeeName + ","
                    + "</h2>"

                    + "<p style='"
                    + "color:#4b5563;"
                    + "font-size:15px;"
                    + "line-height:1.6;"
                    + "'>"
                    + "A verification code has been generated "
                    + "to confirm your attendance."
                    + "</p>"

                    // OTP Box
                    + "<div style='"
                    + "background:#eff6ff;"
                    + "border:1px solid #bfdbfe;"
                    + "border-radius:10px;"
                    + "padding:25px;"
                    + "margin:25px 0;"
                    + "text-align:center;"
                    + "'>"

                    + "<p style='"
                    + "margin:0 0 10px;"
                    + "color:#374151;"
                    + "font-size:14px;"
                    + "'>"
                    + "Your One-Time Password"
                    + "</p>"

                    + "<div style='"
                    + "font-size:36px;"
                    + "font-weight:bold;"
                    + "letter-spacing:8px;"
                    + "color:#2563eb;"
                    + "'>"
                    + otpCode
                    + "</div>"

                    + "<p style='"
                    + "margin:12px 0 0;"
                    + "color:#dc2626;"
                    + "font-size:13px;"
                    + "font-weight:bold;"
                    + "'>"
                    + "This OTP is valid for 2 minutes only."
                    + "</p>"

                    + "</div>"

                    // Security Notice
                    + "<div style='"
                    + "background:#fefce8;"
                    + "border-left:4px solid #eab308;"
                    + "padding:15px;"
                    + "margin-top:20px;"
                    + "'>"

                    + "<p style='"
                    + "margin:0;"
                    + "color:#374151;"
                    + "font-size:14px;"
                    + "line-height:1.6;"
                    + "'>"

                    + "<strong>Security Notice:</strong>"
                    + "<br>"
                    + "Do not share this OTP with anyone."
                    + "<br>"
                    + "The OTP can only be used once."
                    + "<br>"
                    + "If you did not request this verification, "
                    + "please contact the system administrator."

                    + "</p>"

                    + "</div>"

                    + "<p style='"
                    + "color:#4b5563;"
                    + "font-size:14px;"
                    + "line-height:1.6;"
                    + "margin-top:25px;"
                    + "'>"

                    + "Thank you,<br>"
                    + "<strong>Smart Attendance & Payroll System</strong>"

                    + "</p>"

                    + "</div>"

                    // Footer
                    + "<div style='"
                    + "background:#f9fafb;"
                    + "border-top:1px solid #e5e7eb;"
                    + "padding:18px;"
                    + "text-align:center;"
                    + "color:#6b7280;"
                    + "font-size:12px;"
                    + "'>"

                    + "This is an automated email. "
                    + "Please do not reply to this message."

                    + "<br><br>"

                    + "Smart Attendance & Payroll System © 2026"

                    + "</div>"

                    + "</div>"
                    + "</body>"
                    + "</html>";

            // Set HTML content
            helper.setText(
                    emailBody,
                    true
            );

            // Send Email
            mailSender.send(message);

        } catch (MessagingException e) {

            throw new RuntimeException(
                    "Failed to send attendance OTP email.",
                    e
            );
        }
    }
}
