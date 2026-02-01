import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { Theme } from '../../services/theme';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
}

@Component({
  selector: 'app-landing',
  imports: [],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('canvas', { static: false }) canvasRef!: ElementRef<HTMLCanvasElement>;
  
  private ctx!: CanvasRenderingContext2D;
  private particles: Particle[] = [];
  private animationId?: number;
  private mouseX = 0;
  private mouseY = 0;
  
  typewriterText = '';
  private fullText = 'ArchPilot';
  private typewriterIndex = 0;
  private isDeleting = false;
  private typewriterInterval?: any;
  private isPaused = false;

  constructor(
    private themeService: Theme,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.startTypewriter();
  }

  ngAfterViewInit(): void {
    this.initCanvas();
    this.createParticles();
    this.animate();
  }

  ngOnDestroy(): void {
    if (this.animationId) {
      cancelAnimationFrame(this.animationId);
    }
    if (this.typewriterInterval) {
      clearInterval(this.typewriterInterval);
    }
  }

  private initCanvas(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    window.addEventListener('resize', () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      this.createParticles();
    });

    canvas.addEventListener('mousemove', (e) => {
      this.mouseX = e.clientX;
      this.mouseY = e.clientY;
    });
  }

  private createParticles(): void {
    this.particles = [];
    const particleCount = 100;
    
    for (let i = 0; i < particleCount; i++) {
      this.particles.push({
        x: Math.random() * this.canvasRef.nativeElement.width,
        y: Math.random() * this.canvasRef.nativeElement.height,
        vx: (Math.random() - 0.5) * 0.5,
        vy: (Math.random() - 0.5) * 0.5
      });
    }
  }

  private animate(): void {
    this.ctx.clearRect(0, 0, this.canvasRef.nativeElement.width, this.canvasRef.nativeElement.height);
    
    const isDark = this.themeService.getCurrentTheme();
    const color = isDark ? '#ffffff' : '#000000';

    this.particles.forEach((particle, i) => {
      particle.x += particle.vx;
      particle.y += particle.vy;

      const dx = this.mouseX - particle.x;
      const dy = this.mouseY - particle.y;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (distance < 100) {
        particle.x -= dx * 0.01;
        particle.y -= dy * 0.01;
      }

      if (particle.x < 0 || particle.x > this.canvasRef.nativeElement.width) particle.vx *= -1;
      if (particle.y < 0 || particle.y > this.canvasRef.nativeElement.height) particle.vy *= -1;

      this.ctx.beginPath();
      this.ctx.arc(particle.x, particle.y, 2, 0, Math.PI * 2);
      this.ctx.fillStyle = color;
      this.ctx.fill();

      for (let j = i + 1; j < this.particles.length; j++) {
        const dx = this.particles[j].x - particle.x;
        const dy = this.particles[j].y - particle.y;
        const distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 120) {
          this.ctx.beginPath();
          this.ctx.moveTo(particle.x, particle.y);
          this.ctx.lineTo(this.particles[j].x, this.particles[j].y);
          this.ctx.strokeStyle = color;
          this.ctx.globalAlpha = 1 - distance / 120;
          this.ctx.stroke();
          this.ctx.globalAlpha = 1;
        }
      }
    });

    this.animationId = requestAnimationFrame(() => this.animate());
  }

  private startTypewriter(): void {
    this.typewriterInterval = setInterval(() => {
      if (this.isPaused) {
        return;
      }

      if (!this.isDeleting && this.typewriterIndex < this.fullText.length) {
        this.typewriterText += this.fullText[this.typewriterIndex];
        this.typewriterIndex++;
        this.cdr.detectChanges();
      } else if (!this.isDeleting && this.typewriterIndex === this.fullText.length) {
        this.isPaused = true;
        setTimeout(() => { 
          this.isDeleting = true; 
          this.isPaused = false;
        }, 2000);
      } else if (this.isDeleting && this.typewriterIndex > 0) {
        this.typewriterText = this.typewriterText.slice(0, -1);
        this.typewriterIndex--;
        this.cdr.detectChanges();
      } else if (this.isDeleting && this.typewriterIndex === 0) {
        this.isDeleting = false;
        this.isPaused = true;
        setTimeout(() => { 
          this.isPaused = false;
        }, 500);
      }
    }, 150);
  }
}
