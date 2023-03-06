package mail

import Config
import config
import desi.juan.email.api.Email
import desi.juan.email.api.EmailConstants
import desi.juan.email.api.client.ImapClient
import desi.juan.email.api.client.configuration.ClientConfiguration
import desi.juan.email.api.security.TlsConfiguration
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.io.Closeable
import javax.mail.Folder

class IMAPReceiver(val config: Config.EmailConfig) : Closeable {

    private var client: ImapClient? = null

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            client?.disconnect()
        })
    }

    fun getClient(): ImapClient {

        if(client != null){
            return this.client!!
        }

        val tlsConfig = object : TlsConfiguration {}
        val clientConfig = ClientConfiguration(
            10, emptyMap(), 10, tlsConfig, 10
        )
        this.client = ImapClient(
            config.username,
            config.password,
            config.host,
            ImapClient.DEFAULT_IMAPS_PORT.toInt(),
            clientConfig
        )
        return this.client!!
    }

    fun getEmails() : List<Email>{

        val client = getClient()
        val inbox = client.getFolder(EmailConstants.INBOX_FOLDER, Folder.READ_ONLY)
        val emails = client.retrieve(inbox, true)

        return emails
    }

    override fun close(){
        client?.disconnect()
        client = null
    }

}

fun main(){


}

fun main2(){

//    Pop3Receiver().check()
    IMAPReceiver(config().email).apply {
        getEmails().takeLast(1).forEach {
            println(it.subject)

            println(it.body.content)
            println("\n-----\n")
            println(Jsoup.parse(it.body.content).text())

            val js = Jsoup.parse(it.body.content)
            val t = js.textNodes().map {
                it.wholeText
            }.joinToString("\n")
            println(t)
        }
        close()
    }


    val html = """
Newsletter #6: 31.12.2022

Liebe Scheiben-Begeisterte,

mit dem 6. ÖFSV-Newsletter wünschen wir euch allen einen guten Rutsch ins
neue Jahr! Dieses Mal enthalten sind Rückblicke auf nationale und
internationale Turniere, Informationen zum von SAFE Sport angebotenen
kostenlosen Online-Kurs zum Thema “Prävention sexualisierter Gewalt” sowie
zur von SAFE Sport eingerichteten Vertrauensstelle vera*, und Informationen
zum kostenlosen Serviceangebot der NADA. Wir würden uns freuen, wenn ihr
den Newsletter an euren internen Vereinsverteiler weiterleitet! Wenn ihr
Ideen, Wünsche, Anregungen habt, schreibt uns bitte an:
office@frisbeeverband.at.

Mit Klick auf die Überschriften könnt ihr das jeweilige Dokument öffnen und
lesen. Zugriff auf die Dokumente erhaltet ihr über die Drehscheibe. Solltet
ihr noch nicht Mitglied bei der Drehscheibe sein, könnt ihr das gleich
nachholen unter www.frisbeeverband.at/drehscheibe.
Prävention sexualisierter Gewalt - Onlinekurs und Vertrauensstelle vera*
<https://docs.google.com/document/d/19SZlPGM8TZeBhOqrOWEv-oYA6AI72GFwZlQVFbpnmgA/edit#heading=h.8kqbcqvv3vb7>

Geschrieben von: Corinna Uhlenkamp

Hier findet ihr Informationen zum kostenlosen Online-Kurs von SAFE Sport
zum Thema “Prävention sexualisierter Gewalt” und zur von SAFE Sport
eingerichteten Vertrauensstelle vera*.
NADA - Kostenloses Serviceangebot
<https://docs.google.com/document/d/1_5p5NHujkmDV4U_zPhObmpwKm-u96aDCUKItA5ge9hs/edit#heading=h.8kqbcqvv3vb7>

Geschrieben von: Corinna Uhlenkamp

Die Nationale Anti-Doping Agentur Austria GmbH (NADA) stellt im Rahmen
ihrer Präventionsarbeit im Bereich Anti-Doping viele nützliche
Informationen und Services, wie z.B. kostenlose eLearning-Kurse, zur
Verfügung.
Ultimate: Rückblick auf die nationale Saison 2022 - allgemeine Klasse
<https://docs.google.com/document/d/1gXJBPab8YYVAcI7W0htt85yd2RrF5SvN9g1rEW6CFgA/edit#>

Geschrieben von: Andreas Kuhn, Nikolaus (Niki) Jauk und DI Marion
Schneilinger

Hier findet ihr kurze Berichte über die nationalen Saisonhöhepunkte in der
allgemeinen Klasse: ÖStM-X, ÖStM-O/W, BÖStM-X.
Ultimate: Rückblick auf die nationale Saison 2022 - Juniors
<https://docs.google.com/document/d/1iV8ybRN4R0xWrEVMpaoMqAKzKsk18BIbHzEtgmtx9LA/edit#heading=h.o9bgtjcjek0o>

Geschrieben von: Christopher Klambauer und den Ultimate Primates

Hier findet ihr Berichte über die nationalen Highlights der Saison bei den
Junior*innen: JÖM U15 / U17, JÖM U20.
Ultimate: U17 EM und U20 WM
<https://docs.google.com/document/d/1dDvqSWqfn88ULrSI7BuOelkbZPSQdRx1Gxh4ChTZYjg/edit#>

Geschrieben von: Peter Scheruga

Über 1000 Athlet*innen, hochklassige Matches, sensationeller Spirit, und
eine österreichische Delegation, die zu begeistern wusste. Das waren die
kombiniert ausgetragenen Juniors Joint Ultimate Championships in
Breslau/Polen, Anfang August.
Discgolf: WTDGC 2022 - World Team Disc Golf Championships
<https://docs.google.com/document/d/1r99g1eM7cM1wewcxUGhFkSMHRICD6OpIontG_yYkkGQ/edit#heading=h.rj7ba8jn9qjb>

Geschrieben von: Johannes Petz

Die 4. World Team Disc Golf Championships (17. bis 20. August) fanden
ausschließlich im Matchplay-Format statt. Nach einer erfolgreichen
Poolphase verlor Österreichs Team im Achtelfinale gegen den späteren
Vizeweltmeister. Nach weiteren Matchgewinnen war Österreich im Spiel um
Platz 9 gegen GB erfolgreich.
Discgolf: Austro Tour 2022 - Ein Rückblick
<https://docs.google.com/document/d/1MdNSQuCfUK6WcTuWxPeRrICmT1cAfG2QYfRrSHaWGwg/edit#heading=h.8kqbcqvv3vb7>

Geschrieben von: Daniel Maier

Die Discgolf Austro Tour 2022 war ein voller Erfolg. Bei insgesamt 5 großen
Turnieren konnten Punkte für die Gesamtwertung gesammelt werden. Insgesamt
nahmen erfreulicherweise fast 200 verschiedene Sportler*innen an Turnieren
der Austro Tour teil.
Discgolf: Neue Website 2022
<https://docs.google.com/document/d/1qtEEpIwAdNjuwwpPToqJabRrZ3V4y2LJvsjOSqJwCC0/edit#>

Geschrieben von: Johannes Petz

Der ÖDGV freut sich über einen neuen Internetauftritt.
Links
<https://docs.google.com/document/d/1szFAN-NscX2_PR8lG-WFUcn2jnnk6hRh6AnSPgfomPQ/edit#heading=h.o9bgtjcjek0o>

Hier findet ihr den Hinweis zum Thema Förderung von sportwissenschaftlichen
Arbeiten durch den ÖFSV und die Links zum Sport Austria
Fortbildungskalender und den ÖFSV Vorstandsprotokollen.


Alles Liebe und ein gutes neues Jahr

Euer ÖFSV



*Österr. Frisbee-Sport Verband*
Mag. Corinna Uhlenkamp, MSc
Geschäftsführerin

ZVR: 297193118
Hansl-Schmid-Weg 1
A-1160 Wien
+43 650 4404490
www.frisbeeverband.at


-- 
P.s.: Wie kann dich der ÖFSV unterstützen?
In Zukunft soll es von Themenbeauftragten im ÖFSV online Meetings geben, in
denen Sie für euch zur Verfügung stehen. Damit dies vorbereitet werden kann
worauf, geht bitte zu diesem Formular und lasst uns Wissen was wir für euch
tun können.
https://forms.gle/rkFk6F5DAjgRmpJ28
---
Sie erhalten diese Nachricht, weil Sie in Google Groups E-Mails von der
Gruppe "Drehscheibe" abonniert haben.
Wenn Sie sich von dieser Gruppe abmelden und keine E-Mails mehr von dieser
Gruppe erhalten möchten, senden Sie eine E-Mail an
drehscheibe+unsubscribe@frisbeeverband.at.
Wenn Sie diese Diskussion im Web verfolgen möchten, rufen Sie
https://groups.google.com/a/frisbeeverband.at/d/msgid/drehscheibe/CAJZCy%2B_BqW2EEBDR6Vn3R2kJ%2BW3JZ5f2jRhxDyGLsZy7%2BxzPUA%40mail.gmail.com
<https://groups.google.com/a/frisbeeverband.at/d/msgid/drehscheibe/CAJZCy%2B_BqW2EEBDR6Vn3R2kJ%2BW3JZ5f2jRhxDyGLsZy7%2BxzPUA%40mail.gmail.com?utm_medium=email&utm_source=footer>
auf.

<div dir="ltr"><br><br><div class="gmail_quote"><div dir="ltr" class="gmail_attr">---------- Forwarded message ---------<br>Von: <strong class="gmail_sendername" dir="auto">Corinna Uhlenkamp</strong> <span dir="auto">&lt;<a href="mailto:office@frisbeeverband.at">office@frisbeeverband.at</a>&gt;</span><br>Date: Sa., 31. Dez. 2022 um 16:35 Uhr<br>Subject: [drehscheibe] ÖFSV-Newsletter #6 - Wir wünschen euch einen guten Rutsch ins neue Jahr!<br>To:  &lt;<a href="mailto:drehscheibe@frisbeeverband.at">drehscheibe@frisbeeverband.at</a>&gt;<br></div><br><br><div dir="ltr"><div><span id="m_8821051462622730471gmail-docs-internal-guid-ff8b1cda-7fff-bd28-7c53-e5b86321440a" style="font-family:verdana,sans-serif;color:rgb(0,0,0)"><p dir="ltr" style="line-height:1.38;border-bottom-width:2.25pt;border-bottom-style:solid;border-bottom-color:rgb(60,60,60);margin-top:0pt;margin-bottom:3pt;padding:0pt 0pt 2pt"><span style="font-size:26pt;font-family:Verdana;color:rgb(60,60,60);font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Newsletter #6: 31.12.2022</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Liebe Scheiben-Begeisterte,</span></p><br><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">mit dem 6. ÖFSV-Newsletter wünschen wir euch allen einen guten Rutsch ins neue Jahr! Dieses Mal enthalten sind Rückblicke auf nationale und internationale Turniere, Informationen zum von SAFE Sport angebotenen kostenlosen Online-Kurs zum Thema “Prävention sexualisierter Gewalt” sowie zur von SAFE Sport eingerichteten Vertrauensstelle vera*, und Informationen zum kostenlosen Serviceangebot der NADA. Wir würden uns freuen, wenn ihr den Newsletter an euren internen Vereinsverteiler weiterleitet! Wenn ihr Ideen, Wünsche, Anregungen habt, schreibt uns bitte an: </span><a><span style="font-size:11pt;font-family:Verdana;color:rgb(17,85,204);font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">office@frisbeeverband.at</span></a><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">. </span></p><br><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Mit Klick auf die Überschriften könnt ihr das jeweilige Dokument öffnen und lesen. Zugriff auf die Dokumente erhaltet ihr über die Drehscheibe. Solltet ihr noch nicht Mitglied bei der Drehscheibe sein, könnt ihr das gleich nachholen unter </span><a href="http://www.frisbeeverband.at/drehscheibe" style="text-decoration:none" target="_blank"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">www.frisbeeverband.at/drehscheibe</span></a><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">. </span></p><h2 dir="ltr" style="line-height:1.38;margin-top:18pt;margin-bottom:6pt"><a href="https://docs.google.com/document/d/19SZlPGM8TZeBhOqrOWEv-oYA6AI72GFwZlQVFbpnmgA/edit#heading=h.8kqbcqvv3vb7" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-weight:400;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">Prävention sexualisierter Gewalt - Onlinekurs und Vertrauensstelle vera*</span></a></h2><p dir="ltr" style="line-height:1.38;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Geschrieben von: Corinna Uhlenkamp</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:16pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Hier findet ihr Informationen zum kostenlosen Online-Kurs von SAFE Sport zum Thema “Prävention sexualisierter Gewalt” und zur von SAFE Sport eingerichteten Vertrauensstelle vera*.</span></p><h2 dir="ltr" style="line-height:1.38;margin-top:18pt;margin-bottom:6pt"><a href="https://docs.google.com/document/d/1_5p5NHujkmDV4U_zPhObmpwKm-u96aDCUKItA5ge9hs/edit#heading=h.8kqbcqvv3vb7" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-weight:400;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">NADA - Kostenloses Serviceangebot</span></a></h2><p dir="ltr" style="line-height:1.38;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Geschrieben von: Corinna Uhlenkamp</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:16pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Die Nationale Anti-Doping Agentur Austria GmbH (NADA) stellt im Rahmen ihrer Präventionsarbeit im Bereich Anti-Doping viele nützliche Informationen und Services, wie z.B. kostenlose eLearning-Kurse, zur Verfügung.</span></p><h2 dir="ltr" style="line-height:1.38;margin-top:18pt;margin-bottom:6pt"><a href="https://docs.google.com/document/d/1gXJBPab8YYVAcI7W0htt85yd2RrF5SvN9g1rEW6CFgA/edit#" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-weight:400;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">Ultimate: Rückblick auf die nationale Saison 2022 - allgemeine Klasse</span></a></h2><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Geschrieben von: Andreas Kuhn, Nikolaus (Niki) Jauk und DI Marion Schneilinger</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:16pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Hier findet ihr kurze Berichte über die nationalen Saisonhöhepunkte in der allgemeinen Klasse: ÖStM-X, ÖStM-O/W, BÖStM-X.</span></p><h2 dir="ltr" style="line-height:1.38;margin-top:18pt;margin-bottom:6pt"><a href="https://docs.google.com/document/d/1iV8ybRN4R0xWrEVMpaoMqAKzKsk18BIbHzEtgmtx9LA/edit#heading=h.o9bgtjcjek0o" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-weight:400;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">Ultimate: Rückblick auf die nationale Saison 2022 - Juniors</span></a></h2><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Geschrieben von: Christopher Klambauer und den Ultimate Primates</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:16pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Hier findet ihr Berichte über die nationalen Highlights der Saison bei den Junior*innen: JÖM U15 / U17, JÖM U20.</span></p><h2 dir="ltr" style="line-height:1.38;margin-top:18pt;margin-bottom:6pt"><a href="https://docs.google.com/document/d/1dDvqSWqfn88ULrSI7BuOelkbZPSQdRx1Gxh4ChTZYjg/edit#" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-weight:400;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">Ultimate: U17 EM und U20 WM</span></a></h2><p dir="ltr" style="line-height:1.38;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Geschrieben von: Peter Scheruga</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:16pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Über 1000 Athlet*innen, hochklassige Matches, sensationeller Spirit, und eine österreichische Delegation, die zu begeistern wusste. Das waren die kombiniert ausgetragenen Juniors Joint Ultimate Championships in Breslau/Polen, Anfang August.</span></p><h2 dir="ltr" style="line-height:1.38;margin-top:18pt;margin-bottom:6pt"><a href="https://docs.google.com/document/d/1r99g1eM7cM1wewcxUGhFkSMHRICD6OpIontG_yYkkGQ/edit#heading=h.rj7ba8jn9qjb" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-weight:400;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">Discgolf: WTDGC 2022 - World Team Disc Golf Championships</span></a></h2><p dir="ltr" style="line-height:1.38;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Geschrieben von: Johannes Petz</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt;padding:0pt 0pt 11pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Die 4. World Team Disc Golf Championships (17. bis 20. August) fanden ausschließlich im Matchplay-Format statt. Nach einer erfolgreichen Poolphase verlor Österreichs Team im Achtelfinale gegen den späteren Vizeweltmeister. Nach weiteren Matchgewinnen war Österreich im Spiel um Platz 9 gegen GB erfolgreich</span><span style="font-size:11pt;font-family:Verdana;font-style:italic;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">.</span></p><h2 dir="ltr" style="line-height:1.38;margin-top:18pt;margin-bottom:6pt"><a href="https://docs.google.com/document/d/1MdNSQuCfUK6WcTuWxPeRrICmT1cAfG2QYfRrSHaWGwg/edit#heading=h.8kqbcqvv3vb7" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-weight:400;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">Discgolf: Austro Tour 2022 - Ein Rückblick</span></a></h2><p dir="ltr" style="line-height:1.38;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Geschrieben von: Daniel Maier</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:16pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Die Discgolf Austro Tour 2022 war ein voller Erfolg. Bei insgesamt 5 großen Turnieren konnten Punkte für die Gesamtwertung gesammelt werden. Insgesamt nahmen erfreulicherweise fast 200 verschiedene Sportler*innen an Turnieren der Austro Tour teil.</span></p><a href="https://docs.google.com/document/d/1qtEEpIwAdNjuwwpPToqJabRrZ3V4y2LJvsjOSqJwCC0/edit#" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">Discgolf: Neue Website 2022</span></a><br><p dir="ltr" style="line-height:1.38;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Geschrieben von: Johannes Petz</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:16pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Der ÖDGV freut sich über einen neuen Internetauftritt.</span></p><h2 dir="ltr" style="line-height:1.38;margin-top:18pt;margin-bottom:6pt"><a href="https://docs.google.com/document/d/1szFAN-NscX2_PR8lG-WFUcn2jnnk6hRh6AnSPgfomPQ/edit#heading=h.o9bgtjcjek0o" style="text-decoration:none" target="_blank"><span style="font-size:16pt;font-family:Verdana;color:rgb(180,45,41);font-weight:400;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;text-decoration:underline;vertical-align:baseline;white-space:pre-wrap">Links</span></a></h2><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Hier findet ihr den Hinweis zum Thema Förderung von sportwissenschaftlichen Arbeiten durch den ÖFSV und die Links zum Sport Austria Fortbildungskalender und den ÖFSV Vorstandsprotokollen.</span></p><p dir="ltr" style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap"><br></span></p><p style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Alles Liebe und ein gutes neues Jahr</span></p><p style="line-height:1.38;text-align:justify;margin-top:0pt;margin-bottom:0pt"><span style="font-size:11pt;font-family:Verdana;font-variant-ligatures:normal;font-variant-numeric:normal;font-variant-alternates:normal;font-variant-east-asian:normal;vertical-align:baseline;white-space:pre-wrap">Euer ÖFSV  </span></p></span><div style="font-family:verdana,sans-serif"><br></div></div><div><br style="color:rgb(0,0,0)"></div><div><div dir="ltr" data-smartmail="gmail_signature"><div dir="ltr"><div dir="ltr"><div dir="ltr"><div dir="ltr"><p class="MsoNormal"><b><span style="font-size:10.5pt;font-family:Verdana,sans-serif;color:black">Österr. Frisbee-Sport Verband<br></span></b><span style="color:black;font-family:Verdana,sans-serif"><br><span style="font-size:10.5pt">Mag. Corinna Uhlenkamp, MSc</span><span style="font-size:14px"><br></span></span><span style="font-size:9pt;color:rgb(0,0,0);font-family:Verdana,sans-serif">Geschäftsführerin<br><br></span></p>

<p class="MsoNormal"><span style="font-size:9pt;font-family:Verdana,sans-serif;color:black">ZVR: 297193118<br>Hansl-Schmid-Weg 1<br></span><span style="color:black;font-family:Verdana,sans-serif;font-size:9pt">A-1160 Wien<br></span><span style="font-family:Verdana,sans-serif;font-size:9pt"><font color="#000000">+43 650 4404490<br></font></span><a href="http://www.frisbeeverband.at/" style="font-family:Verdana,sans-serif;font-size:9pt" target="_blank">www.frisbeeverband.at</a><br><br></p><p class="MsoNormal"><img src="https://ci3.googleusercontent.com/mail-sig/AIorK4yv8G4S5XBfSNRmHOwOEvZFBFei7u4lBslepHPMBXxH5n8K5XfDsgLAFZIr7yxu83X3I43n_y0"><br></p></div></div></div></div></div></div></div>

<p></p>

-- <br>
P.s.: Wie kann dich der ÖFSV unterstützen?<br>
In Zukunft soll es von Themenbeauftragten im ÖFSV online Meetings geben, in denen Sie für euch zur Verfügung stehen. Damit dies vorbereitet werden kann worauf, geht bitte zu diesem Formular und lasst uns Wissen was wir für euch tun können.<br>
<a href="https://forms.gle/rkFk6F5DAjgRmpJ28" target="_blank">https://forms.gle/rkFk6F5DAjgRmpJ28</a><br>
--- <br>
Sie erhalten diese Nachricht, weil Sie in Google Groups E-Mails von der Gruppe &quot;Drehscheibe&quot; abonniert haben.<br>
Wenn Sie sich von dieser Gruppe abmelden und keine E-Mails mehr von dieser Gruppe erhalten möchten, senden Sie eine E-Mail an <a href="mailto:drehscheibe+unsubscribe@frisbeeverband.at" target="_blank">drehscheibe+unsubscribe@frisbeeverband.at</a>.<br>
Wenn Sie diese Diskussion im Web verfolgen möchten, rufen Sie <a href="https://groups.google.com/a/frisbeeverband.at/d/msgid/drehscheibe/CAJZCy%2B_BqW2EEBDR6Vn3R2kJ%2BW3JZ5f2jRhxDyGLsZy7%2BxzPUA%40mail.gmail.com?utm_medium=email&amp;utm_source=footer" target="_blank">https://groups.google.com/a/frisbeeverband.at/d/msgid/drehscheibe/CAJZCy%2B_BqW2EEBDR6Vn3R2kJ%2BW3JZ5f2jRhxDyGLsZy7%2BxzPUA%40mail.gmail.com</a> auf.<br>
</div></div>"""


    val js = Jsoup.parse(html)
//    val els = js.allElements.toList()
////    .filter {
////        it.tag().normalName() in listOf("h1", "h2", "h3", H4)
////    }
//        .map { it.wholeText() }
//    val t = js.textNodes().map {
//        it.wholeText
//    }.joinToString("\n")
//    println(t)

    val accum = StringUtil.borrowBuilder()

    NodeTraversor.traverse(object : NodeVisitor {

        override fun head(node: Node, depth: Int) {

            if (node is TextNode) {

                accum.append(node.wholeText)

            } else if (node is Element) {

                val name = node.tag().normalName()
                if (name in listOf("h1", "h2", "h3", "h4")) accum.append("**")

                when {
                    name == "a" -> {
                        val link = node.attr("href")
                        if(link.isNotBlank()){
                            accum.append("<$link>\n")
                        }
                    }
                    name in listOf("br", "p") -> {
                        accum.append("\n")
                    }

                }
            }
        }

        override fun tail(node: Node, depth: Int) {
            if(node is Element){
                if (node.tag().normalName() in listOf("h1", "h2", "h3", "h4")) accum.append("**\n")
            }
            super.tail(node, depth)
        }

    }, js.root())

    val text = StringUtil.releaseBuilder(accum)
        .replace("\u00a0","")

    println("")
}