import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { TransformerDetailResponse, MeterDetail } from '../types/energy';
import { AVERAGE_TARIFF_MZN, TECHNICAL_LOSS_THRESHOLD } from '../constants/energy';

export const generateAuditPdf = (detail: TransformerDetailResponse) => {
  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
  });

  const isCritical = detail.isCriticalLoss;
  const primaryColor: [number, number, number] = isCritical ? [185, 28, 28] : [15, 118, 110]; // Red or Teal
  const darkSlate: [number, number, number] = [15, 23, 42];

  // Header Banner
  doc.setFillColor(darkSlate[0], darkSlate[1], darkSlate[2]);
  doc.rect(0, 0, 210, 30, 'F');

  // Accent line
  doc.setFillColor(primaryColor[0], primaryColor[1], primaryColor[2]);
  doc.rect(0, 29, 210, 2, 'F');

  // Title EDM
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(13);
  doc.text('ELECTRICIDADE DE MOÇAMBIQUE, E.P. (EDM)', 14, 12);

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  doc.setTextColor(203, 213, 225);
  doc.text('Direcção de Distribuição e Auditoria de Perdas Não Técnicas • Região Maputo', 14, 18);
  doc.text('Relatório Oficial de Inspecção Técnica & Balanço de Energia', 14, 23);

  // Status Stamp on Top Right
  const emissionDate = new Date().toLocaleDateString('pt-MZ', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });

  doc.setFontSize(7.5);
  doc.setTextColor(148, 163, 184);
  doc.text(`Emissão: ${emissionDate}`, 196, 12, { align: 'right' });
  doc.text(`Protocolo: AUD-${detail.transformerCode.replace(/[^a-zA-Z0-9]/g, '')}-${Date.now().toString().slice(-4)}`, 196, 17, { align: 'right' });
  doc.text('Classificação: CONFIDENCIAL / USO INTERNO', 196, 22, { align: 'right' });

  // Section 1: Posto de Transformação Info
  let currentY = 38;

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(11);
  doc.setTextColor(30, 41, 59);
  doc.text('1. DADOS TÉCNICOS DO POSTO DE TRANSFORMAÇÃO (PT)', 14, currentY);

  currentY += 4;
  doc.setDrawColor(226, 232, 240);
  doc.line(14, currentY, 196, currentY);

  currentY += 6;
  doc.setFontSize(8.5);

  // 2-column info grid
  const col1X = 14;
  const col2X = 110;

  // Box 1
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(71, 85, 105);
  doc.text('Código do PT:', col1X, currentY);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(15, 23, 42);
  doc.text(`${detail.transformerCode}`, col1X + 30, currentY);

  doc.setFont('helvetica', 'bold');
  doc.setTextColor(71, 85, 105);
  doc.text('Bairro / Zona:', col2X, currentY);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(15, 23, 42);
  doc.text(`${detail.neighborhood} (Maputo)`, col2X + 28, currentY);

  currentY += 6;
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(71, 85, 105);
  doc.text('Capacidade:', col1X, currentY);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(15, 23, 42);
  doc.text(`${detail.capacityKva} kVA`, col1X + 30, currentY);

  doc.setFont('helvetica', 'bold');
  doc.setTextColor(71, 85, 105);
  doc.text('Total Medidores:', col2X, currentY);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(15, 23, 42);
  doc.text(`${detail.metersCount} unidades consumidoras`, col2X + 28, currentY);

  currentY += 6;
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(71, 85, 105);
  doc.text('Energia Injetada (PT):', col1X, currentY);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(15, 23, 42);
  doc.text(`${detail.totalSuppliedKwh.toLocaleString('pt-MZ')} kWh`, col1X + 35, currentY);

  doc.setFont('helvetica', 'bold');
  doc.setTextColor(71, 85, 105);
  doc.text('Consumo Faturado:', col2X, currentY);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(15, 23, 42);
  doc.text(`${detail.totalBilledKwh.toLocaleString('pt-MZ')} kWh`, col2X + 33, currentY);

  // Section 2: Diagnóstico de Balanço Energético & Perdas
  currentY += 12;
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(11);
  doc.setTextColor(30, 41, 59);
  doc.text('2. BALANÇO ENERGÉTICO E AVALIAÇÃO DE CONFORMIDADE', 14, currentY);

  currentY += 4;
  doc.setDrawColor(226, 232, 240);
  doc.line(14, currentY, 196, currentY);

  currentY += 6;

  // Highlights cards in PDF
  const cardWidth = 58;
  const cardHeight = 22;
  const cardGap = 5;

  // Card 1: Volume de Perda
  const card1X = 14;
  doc.setFillColor(248, 250, 252);
  doc.setDrawColor(226, 232, 240);
  doc.roundedRect(card1X, currentY, cardWidth, cardHeight, 2, 2, 'FD');
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7.5);
  doc.setTextColor(100, 116, 139);
  doc.text('ENERGIA NÃO FATURADA', card1X + 4, currentY + 6);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(11);
  doc.setTextColor(15, 23, 42);
  doc.text(`${detail.lossKwh.toLocaleString('pt-MZ')} kWh`, card1X + 4, currentY + 13);
  doc.setFontSize(7);
  doc.setTextColor(100, 116, 139);
  doc.text(`Meta Técnica: ≤ ${TECHNICAL_LOSS_THRESHOLD.toFixed(1)}%`, card1X + 4, currentY + 18);

  // Card 2: Percentual de Perda
  const card2X = card1X + cardWidth + cardGap;
  doc.setFillColor(isCritical ? 254 : 240, isCritical ? 242 : 253, isCritical ? 242 : 250);
  doc.setDrawColor(isCritical ? 254 : 204, isCritical ? 202 : 251, isCritical ? 202 : 241);
  doc.roundedRect(card2X, currentY, cardWidth, cardHeight, 2, 2, 'FD');
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7.5);
  doc.setTextColor(isCritical ? 153 : 15, isCritical ? 27 : 118, isCritical ? 27 : 110);
  doc.text('ÍNDICE DE PERDA', card2X + 4, currentY + 6);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(12);
  doc.setTextColor(isCritical ? 185 : 13, isCritical ? 28 : 148, isCritical ? 28 : 136);
  doc.text(`${detail.lossPercentage.toFixed(2)}%`, card2X + 4, currentY + 13);
  doc.setFontSize(7);
  doc.text(isCritical ? 'CRÍTICO (Excede limite)' : 'CONFORME (Técnico)', card2X + 4, currentY + 18);

  // Card 3: Prejuízo Financeiro Estimado
  const card3X = card2X + cardWidth + cardGap;
  const totalFinancialLossMt = detail.estimatedFinancialLossMt ?? 0;
  doc.setFillColor(254, 252, 232);
  doc.setDrawColor(254, 240, 138);
  doc.roundedRect(card3X, currentY, cardWidth, cardHeight, 2, 2, 'FD');
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7.5);
  doc.setTextColor(133, 77, 14);
  doc.text('PREJUÍZO FINANCEIRO ESTIMADO', card3X + 4, currentY + 6);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.setTextColor(180, 83, 9);
  doc.text(`${totalFinancialLossMt.toLocaleString('pt-MZ', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} MT`, card3X + 4, currentY + 13);
  doc.setFontSize(7);
  doc.setTextColor(133, 77, 14);
  doc.text(`Tarifa Ref.: ${AVERAGE_TARIFF_MZN.toFixed(2)} MT/kWh`, card3X + 4, currentY + 18);

  currentY += cardHeight + 8;

  // Status Banner
  doc.setFillColor(isCritical ? 254 : 240, isCritical ? 226 : 253, isCritical ? 226 : 244);
  doc.setDrawColor(isCritical ? 239 : 13, isCritical ? 68 : 148, isCritical ? 68 : 136);
  doc.rect(14, currentY, 182, 11, 'FD');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(8);
  doc.setTextColor(isCritical ? 153 : 19, isCritical ? 27 : 78, isCritical ? 27 : 74);
  const statusMsg = isCritical
    ? `PARECER TÉCNICO: ALERTA CRÍTICO DE PERDAS NÃO TÉCNICAS. O índice de perdas (${detail.lossPercentage.toFixed(2)}%) excede a margem de tolerância da EDM de 5,0%. Recomendada intervenção imediata da brigada de fiscalização.`
    : `PARECER TÉCNICO: OPERAÇÃO EM CONFORMIDADE. O índice de perdas (${detail.lossPercentage.toFixed(2)}%) encontra-se abaixo do teto regulamentar de 5,0%. Setor sem indícios de ligações clandestinas generalizadas.`;
  doc.text(statusMsg, 16, currentY + 7, { maxWidth: 178 });

  currentY += 16;

  // Section 3: Tabela de Medidores
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(11);
  doc.setTextColor(30, 41, 59);
  doc.text('3. CADASTRO INDIVIDUALIZADO DE MEDIDORES E ANÁLISE DE CONSUMO', 14, currentY);

  const tableData = detail.meters.map((m: MeterDetail) => {
    const statusLabel =
      m.suspicionStatus === 'SUSPEITO_BYPASS'
        ? 'SUSPEITO BYPASS'
        : m.suspicionStatus === 'CONSUMO_ELEVADO'
        ? 'ELEVADO'
        : 'REGULAR';

    const financialLoss = (m.individualFinancialLossMt ?? 0).toLocaleString('pt-MZ', {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    });

    return [
      m.meterNumber,
      m.residentName + (m.phoneNumber ? `\nTel: ${m.phoneNumber}` : ''),
      m.address,
      `${m.billedKwh.toLocaleString('pt-MZ')} kWh`,
      `${m.individualLossKwh.toLocaleString('pt-MZ', { maximumFractionDigits: 1 })} kWh`,
      `${financialLoss} MT`,
      statusLabel,
    ];
  });

  autoTable(doc, {
    startY: currentY + 4,
    head: [['Nº Medidor', 'Consumidor', 'Endereço', 'Faturado', 'Perda Pro-rata', 'Prejuízo', 'Status']],
    body: tableData,
    theme: 'grid',
    styles: {
      fontSize: 7.5,
      cellPadding: 2,
      textColor: [30, 41, 59],
      valign: 'middle',
    },
    headStyles: {
      fillColor: [15, 23, 42],
      textColor: [255, 255, 255],
      fontStyle: 'bold',
      fontSize: 7.5,
    },
    alternateRowStyles: {
      fillColor: [248, 250, 252],
    },
    columnStyles: {
      0: { fontStyle: 'bold', cellWidth: 24 },
      1: { cellWidth: 38 },
      2: { cellWidth: 42 },
      3: { halign: 'right', cellWidth: 20 },
      4: { halign: 'right', cellWidth: 22 },
      5: { halign: 'right', cellWidth: 18 },
      6: { halign: 'center', cellWidth: 20 },
    },
    didParseCell: (data) => {
      if (data.section === 'body' && data.column.index === 6) {
        const val = String(data.cell.raw);
        if (val.includes('SUSPEITO')) {
          data.cell.styles.textColor = [185, 28, 28];
          data.cell.styles.fontStyle = 'bold';
        } else if (val.includes('ELEVADO')) {
          data.cell.styles.textColor = [180, 83, 9];
          data.cell.styles.fontStyle = 'bold';
        } else {
          data.cell.styles.textColor = [22, 101, 52];
        }
      }
    },
  });

  // After table: Signatures and Audit Stamp
  // @ts-expect-error autoTable adds lastAutoTable to jsPDF instance
  let finalY = (doc.lastAutoTable?.finalY ?? currentY + 50) + 12;

  // If near page bottom, add a new page
  if (finalY > 240) {
    doc.addPage();
    finalY = 25;
  }

  // Section 4: Campo de Assinaturas e Carimbo
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.setTextColor(71, 85, 105);
  doc.text('4. VALIDAÇÃO TÉCNICA E TERMO DE AUDITORIA', 14, finalY);

  finalY += 4;
  doc.setDrawColor(226, 232, 240);
  doc.line(14, finalY, 196, finalY);

  finalY += 18;

  // Signature lines
  const sig1X = 20;
  const sig2X = 120;
  const sigWidth = 70;

  doc.setDrawColor(148, 163, 184);
  doc.line(sig1X, finalY, sig1X + sigWidth, finalY);
  doc.line(sig2X, finalY, sig2X + sigWidth, finalY);

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(8);
  doc.setTextColor(30, 41, 59);
  doc.text('Engenheiro / Auditor Técnico Responsável', sig1X + sigWidth / 2, finalY + 4, { align: 'center' });
  doc.text('Chefe da Brigada de Fiscalização EDM', sig2X + sigWidth / 2, finalY + 4, { align: 'center' });

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7);
  doc.setTextColor(100, 116, 139);
  doc.text('Registo Profissional Ordem dos Engenheiros (OrdEM)', sig1X + sigWidth / 2, finalY + 8, { align: 'center' });
  doc.text('Electricidade de Moçambique - Direcção Regional Maputo', sig2X + sigWidth / 2, finalY + 8, { align: 'center' });

  // Page Numbers
  const pageCount = doc.getNumberOfPages();
  for (let i = 1; i <= pageCount; i++) {
    doc.setPage(i);
    doc.setFontSize(7.5);
    doc.setTextColor(148, 163, 184);
    doc.text(
      `EDM - Auditoria Técnica • PT: ${detail.transformerCode} • Página ${i} de ${pageCount}`,
      105,
      290,
      { align: 'center' }
    );
  }

  // Save the PDF
  const sanitizedCode = detail.transformerCode.replace(/[^a-zA-Z0-9_-]/g, '_');
  doc.save(`Relatorio_Auditoria_EDM_${sanitizedCode}.pdf`);
};
