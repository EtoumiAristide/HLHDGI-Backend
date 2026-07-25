package com.elpandor.hlh.modules.hlh.service;

import com.elpandor.hlh.modules.hlh.model.dto.FneResponse;
import com.elpandor.hlh.modules.hlh.model.dto.ItemDto;
import com.elpandor.hlh.modules.hlh.model.dto.ResumeTaxeDto;
import com.elpandor.hlh.modules.hlh.model.dto.TaxDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.google.gson.Gson;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FnePdfGeneratorService {

    private final TemplateEngine templateEngine;
    private final QrCodeGenerator qrCodeGenerator;

    Gson gson;

    // Infos émetteur : à externaliser en configuration (application.yml),
    // ce ne sont pas des données renvoyées par l'API FNE
//    private final OrganisationDto emetteur;

    public FnePdfGeneratorService(TemplateEngine templateEngine,
                                  QrCodeGenerator qrCodeGenerator) {
        this.templateEngine = templateEngine;
        this.qrCodeGenerator = qrCodeGenerator;
        gson = new Gson();
    }

    public byte[] genererPdf(FneResponse fne, OrganisationDto emetteur) throws Exception {
        //FneResponse fne = gson.fromJson(data, FneResponse.class);
        var invoice = fne.invoice();
        Context ctx = new Context();

        ctx.setVariable("emetteur", emetteur);
        ctx.setVariable("reference", fne.reference());
        ctx.setVariable("dateHeure", formaterDate(invoice.date()));
        ctx.setVariable("modePaiement", libellePaiement(invoice.paymentMethod()));
        ctx.setVariable("pointDeVente", invoice.clientPointOfSale());
        ctx.setVariable("commercialMessage", invoice.commercialMessage());

        ctx.setVariable("clientNom", invoice.clientCompanyName());
        ctx.setVariable("clientNcc", invoice.clientNcc());
        ctx.setVariable("clientRegime", invoice.clientTaxRegime());

        ctx.setVariable("items", invoice.items());
        ctx.setVariable("totalHt", invoice.totalBeforeTaxes());
        ctx.setVariable("totalTva", invoice.vatAmount());
        ctx.setVariable("totalTtc", invoice.totalBeforeTaxes() + invoice.vatAmount());
        ctx.setVariable("autresTaxes", invoice.totalCustomTaxes());
        ctx.setVariable("timbre", invoice.fiscalStamp());
        ctx.setVariable("totalAPayer", invoice.totalDue());

        // Résumé par catégorie de taxe (regroupement + sous-totaux)
        ctx.setVariable("resume", construireResumeTaxes(invoice.items()));

        ctx.setVariable("qrCodeBase64", qrCodeGenerator.genererQrCodeBase64(fne.token()));

        String html = templateEngine.process("facture-fne", ctx);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        // baseUri nécessaire pour résoudre les images statiques (logo FNE, logo entreprise)
        builder.withHtmlContent(html, new File("src/main/resources/static/").toURI().toString());
        builder.toStream(os);
        builder.run();
        return os.toByteArray();
    }

    private List<ResumeTaxeDto> construireResumeTaxes(List<ItemDto> items) {
        Map<String, ResumeTaxeDto> parCategorie = new LinkedHashMap<>();
        for (ItemDto item : items) {
            for (TaxDto taxe : item.taxes()) {
                parCategorie.merge(
                        taxe.name(),
                        new ResumeTaxeDto(taxe.name(), item.amount(), taxe.amount(),
                                item.amount() * taxe.amount() / 100),
                        (a, b) -> new ResumeTaxeDto(a.categorie(),
                                a.sousTotal() + b.sousTotal(), a.taux(),
                                a.totalTaxe() + b.totalTaxe())
                );
            }
        }
        return new ArrayList<>(parCategorie.values());
    }

    private String formaterDate(String isoDate) {
        return OffsetDateTime.parse(isoDate)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    private String libellePaiement(String code) {
        return switch (code) {
            case "cash" -> "Espèces";
            case "card" -> "Carte bancaire";
            case "mobile_money" -> "Mobile Money";
            case "cheque" -> "Chèque";
            case "transfer" -> "Virement";
            default -> code;
        };
    }
}