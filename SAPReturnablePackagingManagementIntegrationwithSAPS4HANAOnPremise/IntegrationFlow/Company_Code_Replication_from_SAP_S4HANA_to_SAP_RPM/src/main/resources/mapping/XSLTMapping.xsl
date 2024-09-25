<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output omit-xml-declaration="yes"/>
<xsl:template match="@xsi:*" 
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" />
<xsl:template match="*">
    <xsl:element name="{local-name(.)}">
     <xsl:apply-templates select="@* | node()"/>
    </xsl:element>
</xsl:template>
<xsl:template match="@*">
    <xsl:attribute name="{local-name(.)}">
      <xsl:value-of select="."/>
    </xsl:attribute>
</xsl:template>
</xsl:stylesheet>
