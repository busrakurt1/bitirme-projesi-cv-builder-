import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

class PDFService {
  /**
   * CV'yi ATS uyumlu PDF'e dönüştürür.
   * - HTML2Canvas ile görüntü tabanlı PDF (Türkçe karakter desteği)
   * - Yüksek kalite ve ATS uyumluluğu
   * - A4 boyutuna göre formatlar
   * @param {Object} user - Kullanıcı verileri (Dosya ismi için)
   * @param {string} elementId - Dönüştürülecek HTML elementinin ID'si
   */
  static async generateCVPDF(user, elementId = 'cv-preview') {
    try {
      console.log('📄 ATS uyumlu PDF oluşturuluyor...');

      const element = document.getElementById(elementId);
      if (!element) throw new Error('CV elementi bulunamadı!');

      // 1. ADIM: KLONLAMA VE TEMİZLİK
      const clone = element.cloneNode(true);
      
      // PDF'te görünmemesi gereken buton/alanları temizle
      clone.querySelectorAll('.pdf-exclude').forEach(el => el.remove());

      // 2. ADIM: GÖRÜNMEZ KONTEYNER (RENDER ALANI)
      const A4_WIDTH_PX = 794; 
      const container = document.createElement('div');
      
      container.style.position = 'fixed';
      container.style.left = '-10000px';
      container.style.top = '0';
      container.style.width = Math.max(element.offsetWidth, A4_WIDTH_PX) + 'px';
      container.style.visibility = 'visible';
      container.style.zIndex = '-9999';
      container.style.backgroundColor = '#ffffff';
      
      container.appendChild(clone);
      document.body.appendChild(container);

      try {
        // 3. ADIM: ASSET YÜKLEME BEKLEMELERİ
        await new Promise(resolve => setTimeout(resolve, 200));

        // Fontların hazır olmasını bekle
        if (document.fonts && document.fonts.ready) {
          await document.fonts.ready;
        }

        // Tüm resimlerin yüklendiğinden emin ol
        const imgs = Array.from(clone.querySelectorAll('img'));
        if (imgs.length > 0) {
          await Promise.all(imgs.map(img => {
            if (img.complete) return Promise.resolve();
            return new Promise(resolve => {
              img.onload = resolve;
              img.onerror = resolve;
              setTimeout(resolve, 3000);
            });
          }));
        }

        // 4. ADIM: HTML2CANVAS İLE GÖRÜNTÜ ALMA (Yüksek kalite, ATS uyumlu)
        const canvas = await html2canvas(clone, {
          scale: 2, // Retina kalitesi
          useCORS: true,
          logging: false,
          backgroundColor: '#ffffff',
          windowWidth: container.scrollWidth,
          windowHeight: container.scrollHeight,
          allowTaint: false,
          removeContainer: false,
          onclone: (clonedDoc) => {
            // Klonlanmış dokümanda tüm linkleri ve butonları temizle
            const clonedElement = clonedDoc.getElementById('cv-preview');
            if (clonedElement) {
              // PDF'te görünmemesi gereken tüm elementleri kaldır
              clonedElement.querySelectorAll('.pdf-exclude').forEach(el => el.remove());
              // Butonları kaldır
              clonedElement.querySelectorAll('button').forEach(el => el.remove());
            }
          }
        });

        // 5. ADIM: PDF OLUŞTURMA (ATS uyumlu, sayfalama iyileştirildi)
        const imgData = canvas.toDataURL('image/png', 1.0);
        const pdf = new jsPDF('p', 'mm', 'a4');
        
        const pdfWidth = pdf.internal.pageSize.getWidth();   // 210mm
        const pdfHeight = pdf.internal.pageSize.getHeight(); // 297mm

        // Canvas boyutlarını kontrol et
        if (!canvas || canvas.width === 0 || canvas.height === 0) {
          throw new Error('Canvas oluşturulamadı veya geçersiz boyutlar');
        }

        // Canvas piksellerini PDF milimetresine çevirme
        const imgAspectRatio = canvas.width / canvas.height;
        
        let imgWidthInPDF = pdfWidth;
        let imgHeightInPDF = pdfWidth / imgAspectRatio;
        
        // Sayfalama - daha iyi görünüm için
        let heightLeft = imgHeightInPDF;
        let position = 0;
        let pageNumber = 1;
        const maxPages = 5; // Maksimum 5 sayfa

        // İlk sayfayı bas
        pdf.addImage(imgData, 'PNG', 0, position, imgWidthInPDF, imgHeightInPDF);
        heightLeft -= pdfHeight;

        // Taşma varsa yeni sayfalar ekle (daha düzgün sayfalama)
        while (heightLeft > 0 && pageNumber < maxPages) {
          position -= pdfHeight;
          pdf.addPage();
          pdf.addImage(imgData, 'PNG', 0, position, imgWidthInPDF, imgHeightInPDF);
          heightLeft -= pdfHeight;
          pageNumber++;
        }
        
        // Eğer hala taşma varsa, son kısmı ayrı sayfaya al
        if (heightLeft > 0 && pageNumber < maxPages) {
          // Son kısmı ayrı sayfada göster
          const remainingHeight = Math.min(heightLeft, pdfHeight);
          pdf.addPage();
          pdf.addImage(imgData, 'PNG', 0, -(imgHeightInPDF - remainingHeight), imgWidthInPDF, imgHeightInPDF);
        }

        // 6. ADIM: LİNKLERİ EKLEME (ATS uyumlu - sadece geçerli linkler)
        try {
          const links = clone.querySelectorAll('a[href]');
          const cloneRect = clone.getBoundingClientRect();

          links.forEach(link => {
            try {
              const href = link.getAttribute('href');
              if (!href || href === '#' || href.startsWith('javascript:')) {
                return; // Geçersiz linkleri atla
              }

              const linkRect = link.getBoundingClientRect();
              const relativeX_Px = linkRect.left - cloneRect.left;
              const relativeY_Px = linkRect.top - cloneRect.top;
              const w_Px = linkRect.width;
              const h_Px = linkRect.height;
              
              if (w_Px <= 0 || h_Px <= 0) {
                return; // Geçersiz boyutları atla
              }
              
              const domScaleFactor = pdfWidth / Math.max(clone.offsetWidth, 1);
              const pdfX = Math.max(0, relativeX_Px * domScaleFactor);
              const pdfY = Math.max(0, relativeY_Px * domScaleFactor);
              const pdfW = w_Px * domScaleFactor;
              const pdfH = h_Px * domScaleFactor;
              
              const linkPageNumber = Math.max(1, Math.floor(pdfY / pdfHeight) + 1);
              const linkYOnPage = pdfY - ((linkPageNumber - 1) * pdfHeight);

              if (linkPageNumber > 0 && linkPageNumber <= pdf.getNumberOfPages()) {
                pdf.setPage(linkPageNumber);
                pdf.link(pdfX, linkYOnPage, pdfW, pdfH, { url: href });
              }
            } catch (linkError) {
              console.warn('Link eklenirken hata:', linkError);
              // Link hatası olsa bile devam et
            }
          });
        } catch (linkError) {
          console.warn('Link işleme hatası:', linkError);
          // Link işleme hatası olsa bile PDF oluşturmayı devam ettir
        }

        // 7. ADIM: KAYDETME (ATS uyumlu dosya adı)
        const safeName = (user?.fullName || user?.adSoyad || 'CV')
          .replace(/\s+/g, '_')
          .replace(/[^a-zA-Z0-9_]/g, '')
          .substring(0, 50); // Maksimum 50 karakter
        const fileName = `${safeName}_ATS_CV.pdf`;
        
        // PDF metadata'sını ATS uyumlu hale getir
        pdf.setProperties({
          title: `${safeName} - CV`,
          subject: 'CV - Resume',
          author: safeName,
          creator: 'CV Builder',
          producer: 'CV Builder'
        });
        
        pdf.save(fileName);
        
        console.log(`✅ ATS uyumlu PDF başarıyla oluşturuldu: ${fileName}`);

      } finally {
        // Temizlik
        if (document.body.contains(container)) {
          document.body.removeChild(container);
        }
      }

    } catch (error) {
      console.error('❌ PDF Oluşturma Hatası:', error);
      alert('PDF oluşturulurken bir hata oluştu: ' + error.message);
    }
  }
}

export default PDFService;