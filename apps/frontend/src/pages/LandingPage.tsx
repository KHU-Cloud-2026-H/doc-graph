import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { NOTION_LOGIN_URL } from '../lib/apiClient';

const NotionMark = ({
  className,
  width,
  height,
}: {
  className?: string;
  width?: number;
  height?: number;
}) => (
  <svg className={className} width={width} height={height} viewBox="0 0 128 128" fill="none" aria-hidden="true">
    <path
      fillRule="evenodd"
      clipRule="evenodd"
      d="M76.9829 16.2189L23.7438 20.1417C19.4488 20.5106 17.9541 23.314 17.9541 26.6691V84.9025C17.9541 87.5199 18.8843 89.7555 21.1296 92.7418L33.6455 108.982C35.6984 111.596 37.5684 112.157 41.4944 111.965L103.32 108.234C108.549 107.862 110.047 105.434 110.047 101.328V35.8171C110.047 33.6937 109.206 33.081 106.73 31.2752C106.589 31.1716 106.448 31.0679 106.307 30.964L89.3128 19.0191C85.2039 16.036 83.5168 15.6575 76.9829 16.2189ZM42.8961 34.7426C37.8474 35.0826 36.7023 35.1595 33.8348 32.8308L26.544 27.0444C25.7998 26.297 26.1751 25.3636 28.0419 25.1808L79.225 21.4472C83.52 21.0719 85.7621 22.5698 87.4428 23.8721L96.2219 30.2199C96.594 30.4059 97.5274 31.5254 96.408 31.5254L43.5473 34.7009L42.8961 34.7426ZM37.007 100.767V45.1479C37.007 42.723 37.7544 41.6035 39.9965 41.4143L100.703 37.8699C102.762 37.6839 103.696 38.9926 103.696 41.4143V96.6582C103.696 99.0895 103.32 101.142 99.9555 101.328L41.8665 104.69C38.4986 104.876 37.007 103.76 37.007 100.767ZM94.3583 48.1309C94.7304 49.8149 94.3583 51.4957 92.6712 51.6849L89.8742 52.243V93.2999C87.4428 94.6086 85.2007 95.3527 83.3339 95.3527C80.3445 95.3527 79.5971 94.4225 77.355 91.6223L59.0494 62.8825V90.6921L64.8423 91.9976C64.8423 91.9976 64.8423 95.3559 60.1657 95.3559L47.2809 96.1033C46.9056 95.3559 47.2809 93.4891 48.5896 93.1171L51.9511 92.1869V55.4121L47.2809 55.0368C46.9088 53.3561 47.839 50.9311 50.4564 50.7451L64.281 49.8149L83.3339 78.93V53.1732L78.4777 52.6151C78.1024 50.5591 79.5971 49.0643 81.4639 48.8815L94.3583 48.1309Z"
      fill="currentColor"
    />
  </svg>
);

const styles = `
.dgLanding {
  --blue-600: #2563eb;
  --blue-700: #1d4ed8;
  --blue-100: #dbeafe;
  --blue-50: #eff6ff;
  --slate-900: #0f172a;
  --slate-800: #1e293b;
  --slate-700: #334155;
  --slate-600: #475569;
  --slate-500: #64748b;
  --slate-400: #94a3b8;
  --slate-300: #cbd5e1;
  --slate-200: #e2e8f0;
  --slate-100: #f1f5f9;
  --slate-50: #f8fafc;
  --red-600: #dc2626;
  --red-200: #fecaca;
  --red-100: #fee2e2;
  --red-50: #fef2f2;
  --red-800: #991b1b;
  --green-50: #f0fdf4;
  --green-500: #22c55e;
  --green-800: #166534;
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, .06);
  --shadow-md: 0 4px 14px rgba(15, 23, 42, .08);
  --shadow-lg: 0 12px 40px rgba(15, 23, 42, .12);

  font-family: "Pretendard", "Pretendard Variable", -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
  color: var(--slate-900);
  background: #ffffff;
  -webkit-font-smoothing: antialiased;
  line-height: 1.6;
  scroll-behavior: smooth;
}

.dgLanding * {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.dgLanding a {
  text-decoration: none;
  color: inherit;
}

.dgLanding .wrap {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ---------- Buttons ---------- */
.dgLanding .btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color .18s ease, box-shadow .18s ease, transform .18s ease;
  border: 1px solid transparent;
  white-space: nowrap;
}

.dgLanding .btn-primary {
  background: var(--blue-600);
  color: #fff;
  padding: 11px 20px;
  box-shadow: var(--shadow-sm);
}

.dgLanding .btn-primary:hover {
  background: var(--blue-700);
  transform: translateY(-1px);
}

.dgLanding .btn-lg {
  font-size: 16px;
  padding: 14px 26px;
  border-radius: 12px;
}

.dgLanding .btn-ghost {
  background: #fff;
  color: var(--slate-700);
  border-color: var(--slate-200);
  padding: 11px 20px;
}

.dgLanding .btn-ghost:hover {
  background: var(--slate-50);
}

.dgLanding .notion-mark {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

/* ---------- Header ---------- */
.dgLanding header.site {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, .86);
  backdrop-filter: saturate(180%) blur(10px);
  border-bottom: 1px solid var(--slate-200);
}

.dgLanding header.site .row {
  height: 66px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dgLanding .logo {
  font-weight: 800;
  letter-spacing: -.02em;
  font-size: 24px;
  color: var(--slate-900);
}

.dgLanding .logo span {
  color: var(--blue-600);
}

/* ---------- Hero ---------- */
.dgLanding .hero {
  padding: 76px 0 64px;
}

.dgLanding .hero .grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 56px;
  align-items: center;
}

.dgLanding .eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: var(--blue-50);
  color: var(--blue-700);
  font-size: 13px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--blue-100);
  margin-bottom: 22px;
}

.dgLanding .eyebrow .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--blue-600);
}

.dgLanding .hero h1 {
  font-size: 50px;
  line-height: 1.14;
  letter-spacing: -.035em;
  font-weight: 800;
  color: var(--slate-900);
}

.dgLanding .hero h1 .accent {
  color: var(--blue-600);
}

.dgLanding .hero p.sub {
  margin-top: 22px;
  font-size: 18px;
  line-height: 1.65;
  color: var(--slate-500);
  max-width: 520px;
}

.dgLanding .hero .cta {
  margin-top: 34px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.dgLanding .hero .trust {
  margin-top: 20px;
  font-size: 13px;
  color: var(--slate-400);
}

/* ---------- Graph demo card ---------- */
.dgLanding .demo {
  position: relative;
  background: #fff;
  border: 1px solid var(--slate-200);
  border-radius: 20px;
  box-shadow: var(--shadow-lg);
  padding: 14px;
  overflow: hidden;
}

.dgLanding .demo .canvas {
  position: relative;
  border-radius: 14px;
  background-color: var(--slate-50);
  background-image: radial-gradient(circle, var(--slate-300) 1.4px, transparent 1.4px);
  background-size: 30px 30px;
  overflow: hidden;
}

.dgLanding .demo svg.graph {
  display: block;
  width: 100%;
  height: auto;
}

/* Legend */
.dgLanding .legend {
  position: absolute;
  left: 14px;
  bottom: 14px;
  background: rgba(255, 255, 255, .95);
  border: 1px solid var(--slate-200);
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: var(--shadow-md);
  z-index: 5;
}

.dgLanding .legend h5 {
  font-size: 11px;
  font-weight: 700;
  color: var(--slate-800);
  margin-bottom: 8px;
}

.dgLanding .legend .li {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
}

.dgLanding .legend .li:first-of-type {
  margin-top: 0;
}

.dgLanding .legend .li span {
  font-size: 11px;
  color: var(--slate-600);
}

.dgLanding .legend .li span.red {
  color: var(--red-600);
  font-weight: 600;
}

.dgLanding .swatch {
  width: 28px;
  height: 0;
  flex-shrink: 0;
}

.dgLanding .swatch.solid {
  border-top: 2px solid var(--slate-800);
}

.dgLanding .swatch.dashed {
  border-top: 1.5px dashed var(--slate-800);
}

.dgLanding .swatch.conflict {
  border-top: 2px dashed var(--red-600);
}

/* Conflict panel mock (overlay) */
.dgLanding .panel {
  position: absolute;
  right: 16px;
  top: 16px;
  width: 268px;
  z-index: 8;
  background: #fff;
  border: 1px solid var(--slate-200);
  border-radius: 14px;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  font-size: 12px;
}

.dgLanding .panel .ph {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 13px;
  border-bottom: 1px solid var(--slate-200);
}

.dgLanding .panel .ph .ai-ttl {
  font-weight: 700;
  font-size: 12.5px;
  color: var(--slate-900);
  letter-spacing: -.01em;
}

.dgLanding .panel .pbody {
  padding: 12px 13px;
}

.dgLanding .panel .lbl {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  color: var(--slate-900);
  margin-bottom: 7px;
}

.dgLanding .panel .badge-ai {
  font-size: 9px;
  font-weight: 800;
  color: #fff;
  background: var(--blue-600);
  padding: 1px 5px;
  border-radius: 4px;
  letter-spacing: .02em;
}

.dgLanding .panel .cause {
  background: var(--slate-50);
  border: 1px solid var(--slate-200);
  border-radius: 9px;
  padding: 9px 11px;
  color: var(--slate-700);
  line-height: 1.5;
  font-size: 11.5px;
}

.dgLanding .panel .diff {
  margin-top: 12px;
  border: 1px solid var(--slate-200);
  border-radius: 9px;
  overflow: hidden;
  font-family: "SFMono-Regular", ui-monospace, Menlo, monospace;
  font-size: 11px;
}

.dgLanding .panel .diff .line {
  display: flex;
  gap: 9px;
  padding: 8px 11px;
  line-height: 1.45;
}

.dgLanding .panel .diff .del {
  background: var(--red-50);
  color: var(--red-800);
}

.dgLanding .panel .diff .add {
  background: var(--green-50);
  color: var(--green-800);
  border-top: 1px solid var(--slate-200);
}

.dgLanding .panel .diff .sign {
  font-weight: 800;
  user-select: none;
}

.dgLanding .panel .diff .del .sign {
  color: #f87171;
}

.dgLanding .panel .diff .add .sign {
  color: var(--green-500);
}

.dgLanding .panel .pacts {
  display: flex;
  gap: 8px;
  padding: 11px 13px;
  border-top: 1px solid var(--slate-200);
}

.dgLanding .panel .pbtn {
  flex: 1;
  text-align: center;
  font-size: 11.5px;
  font-weight: 700;
  padding: 9px 0;
  border-radius: 8px;
  cursor: pointer;
}

.dgLanding .panel .pbtn.sec {
  background: #fff;
  border: 1px solid var(--slate-200);
  color: var(--slate-700);
}

.dgLanding .panel .pbtn.pri {
  background: var(--blue-600);
  color: #fff;
  border: 1px solid var(--blue-600);
}

/* ---------- Section shells ---------- */
.dgLanding section.block {
  padding: 80px 0;
}

.dgLanding .sec-head {
  text-align: center;
  max-width: 640px;
  margin: 0 auto 52px;
}

.dgLanding .sec-head .kicker {
  font-size: 13px;
  font-weight: 700;
  color: var(--blue-600);
  text-transform: uppercase;
  letter-spacing: .08em;
  margin-bottom: 12px;
}

.dgLanding .sec-head h2 {
  font-size: 34px;
  font-weight: 800;
  letter-spacing: -.025em;
  color: var(--slate-900);
}

.dgLanding .sec-head p {
  margin-top: 14px;
  font-size: 16px;
  color: var(--slate-500);
}

/* Steps */
.dgLanding .steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.dgLanding .step {
  position: relative;
  background: #fff;
  border: 1px solid var(--slate-200);
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: var(--shadow-sm);
}

.dgLanding .step .num {
  position: absolute;
  top: 22px;
  right: 24px;
  font-size: 13px;
  font-weight: 800;
  color: var(--slate-300);
}

.dgLanding .step .ic {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--blue-50);
  color: var(--blue-600);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.dgLanding .step h3 {
  font-size: 17px;
  font-weight: 700;
  color: var(--slate-900);
}

.dgLanding .step p {
  margin-top: 8px;
  font-size: 14px;
  color: var(--slate-500);
  line-height: 1.6;
}

.dgLanding .step-conn {
  display: none;
}

/* Value cards */
.dgLanding .values {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.dgLanding .vcard {
  background: #fff;
  border: 1px solid var(--slate-200);
  border-radius: 16px;
  padding: 30px 26px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow .2s ease, border-color .2s ease, transform .2s ease;
}

.dgLanding .vcard:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--blue-100);
  transform: translateY(-2px);
}

.dgLanding .vcard .ic {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--blue-600);
  color: #fff;
}

.dgLanding .vcard h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--slate-900);
}

.dgLanding .vcard p {
  margin-top: 10px;
  font-size: 14.5px;
  color: var(--slate-500);
  line-height: 1.65;
}

.dgLanding .bg-soft {
  background: var(--slate-50);
}

/* CTA band */
.dgLanding .cta-band {
  padding: 28px 0 96px;
}

.dgLanding .cta-inner {
  background: linear-gradient(135deg, var(--blue-700), var(--blue-600));
  border-radius: 24px;
  padding: 56px 40px;
  text-align: center;
  box-shadow: var(--shadow-lg);
}

.dgLanding .cta-inner h2 {
  color: #fff;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -.025em;
}

.dgLanding .cta-inner p {
  color: var(--blue-100);
  margin-top: 12px;
  font-size: 16px;
}

.dgLanding .cta-inner .btn-white {
  margin-top: 28px;
  background: #fff;
  color: var(--blue-700);
  padding: 14px 28px;
  font-size: 16px;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
}

.dgLanding .cta-inner .btn-white:hover {
  background: var(--slate-50);
  transform: translateY(-1px);
}

/* Footer */
.dgLanding footer.site {
  background: var(--slate-100);
  border-top: 1px solid var(--slate-200);
}

.dgLanding footer.site .row {
  padding: 32px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.dgLanding footer.site .cp {
  font-weight: 700;
  color: var(--slate-500);
  font-size: 14px;
}

.dgLanding footer.site .links {
  display: flex;
  gap: 24px;
}

.dgLanding footer.site .links a {
  color: var(--slate-500);
  font-size: 14px;
  transition: color .18s ease;
}

.dgLanding footer.site .links a:hover {
  color: var(--blue-600);
}

/* ============ GRAPH ANIMATION ============ */
.dgLanding .gnode {
  transform-box: fill-box;
  transform-origin: center;
  opacity: 1;
}

.dgLanding .gnode .card {
  fill: #fff;
  stroke: var(--slate-800);
  stroke-width: 2;
}

.dgLanding .gnode .icn {
  stroke: var(--slate-800);
  stroke-width: 2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.dgLanding .gnode .lbl {
  fill: var(--slate-600);
  font-size: 11px;
  font-weight: 600;
  font-family: inherit;
}

.dgLanding .gnode .lbl.cflbl {
  fill: var(--red-600);
  font-weight: 700;
}

.dgLanding .gedge {
  stroke: var(--slate-800);
  stroke-width: 2;
  fill: none;
  opacity: 1;
  stroke-dashoffset: 0;
}

.dgLanding .gedge.ai {
  stroke-dasharray: 6 5;
}

.dgLanding .cflayer {
  opacity: 1;
}

.dgLanding .cfedge {
  stroke: var(--red-600);
  stroke-width: 2.5;
  fill: none;
  stroke-dasharray: 7 6;
  opacity: 1;
}

.dgLanding .warn {
  transform-box: fill-box;
  transform-origin: center;
  opacity: 1;
}

.dgLanding .warn-ring {
  transform-box: fill-box;
  transform-origin: center;
  opacity: 0;
}

/* node intro keyframes (shared 7s timeline) */
@keyframes dg-n1 {
  0%, 3% { opacity: 0; transform: scale(.82) }
  7%, 96% { opacity: 1; transform: scale(1) }
  100% { opacity: 0; transform: scale(.82) }
}

@keyframes dg-n2 {
  0%, 8% { opacity: 0; transform: scale(.82) }
  12%, 96% { opacity: 1; transform: scale(1) }
  100% { opacity: 0; transform: scale(.82) }
}

@keyframes dg-n3 {
  0%, 13% { opacity: 0; transform: scale(.82) }
  17%, 96% { opacity: 1; transform: scale(1) }
  100% { opacity: 0; transform: scale(.82) }
}

@keyframes dg-n4 {
  0%, 18% { opacity: 0; transform: scale(.82) }
  22%, 96% { opacity: 1; transform: scale(1) }
  100% { opacity: 0; transform: scale(.82) }
}

@keyframes dg-n5 {
  0%, 23% { opacity: 0; transform: scale(.82) }
  27%, 96% { opacity: 1; transform: scale(1) }
  100% { opacity: 0; transform: scale(.82) }
}

@keyframes dg-n6 {
  0%, 28% { opacity: 0; transform: scale(.82) }
  32%, 96% { opacity: 1; transform: scale(1) }
  100% { opacity: 0; transform: scale(.82) }
}

/* edge draw keyframes (dashoffset 700 -> 0) */
@keyframes dg-d1 {
  0%, 32% { opacity: 0; stroke-dashoffset: 700 }
  33% { opacity: 1 }
  41%, 96% { opacity: 1; stroke-dashoffset: 0 }
  100% { opacity: 0 }
}

@keyframes dg-d2 {
  0%, 36% { opacity: 0; stroke-dashoffset: 700 }
  37% { opacity: 1 }
  45%, 96% { opacity: 1; stroke-dashoffset: 0 }
  100% { opacity: 0 }
}

@keyframes dg-d3 {
  0%, 40% { opacity: 0; stroke-dashoffset: 700 }
  41% { opacity: 1 }
  49%, 96% { opacity: 1; stroke-dashoffset: 0 }
  100% { opacity: 0 }
}

@keyframes dg-aiedgein {
  0%, 44% { opacity: 0 }
  53%, 96% { opacity: 1 }
  100% { opacity: 0 }
}

@keyframes dg-d5 {
  0%, 48% { opacity: 0; stroke-dashoffset: 700 }
  49% { opacity: 1 }
  57%, 96% { opacity: 1; stroke-dashoffset: 0 }
  100% { opacity: 0 }
}

@keyframes dg-d6 {
  0%, 52% { opacity: 0; stroke-dashoffset: 700 }
  53% { opacity: 1 }
  61%, 96% { opacity: 1; stroke-dashoffset: 0 }
  100% { opacity: 0 }
}

@keyframes dg-redborder {
  0%, 62% { opacity: 0 }
  67%, 96% { opacity: 1 }
  100% { opacity: 0 }
}

@keyframes dg-cfedgein {
  0%, 64% { opacity: 0 }
  72%, 96% { opacity: 1 }
  100% { opacity: 0 }
}

@keyframes dg-march {
  to { stroke-dashoffset: -26 }
}

@keyframes dg-warnIn {
  0%, 72% { opacity: 0; transform: scale(.5) }
  77%, 96% { opacity: 1; transform: scale(1) }
  100% { opacity: 0; transform: scale(.5) }
}

@keyframes dg-ring {
  0%, 73% { opacity: 0; transform: scale(.5) }
  77% { opacity: .55; transform: scale(.6) }
  84% { opacity: 0; transform: scale(1.7) }
  90% { opacity: .55; transform: scale(.6) }
  96% { opacity: 0; transform: scale(1.7) }
  100% { opacity: 0 }
}

@keyframes dg-panelIn {
  0%, 72% { opacity: 0; transform: translateY(14px) }
  79%, 96% { opacity: 1; transform: translateY(0) }
  100% { opacity: 0; transform: translateY(14px) }
}

@keyframes dg-cflabel {
  0%, 64% { fill: #475569; font-weight: 600 }
  72%, 96% { fill: #dc2626; font-weight: 700 }
  100% { fill: #475569; font-weight: 600 }
}

/* attach animations */
.dgLanding .anim .gnode.a1 { animation: dg-n1 7s ease-in-out infinite }
.dgLanding .anim .gnode.a2 { animation: dg-n2 7s ease-in-out infinite }
.dgLanding .anim .gnode.a3 { animation: dg-n3 7s ease-in-out infinite }
.dgLanding .anim .gnode.a4 { animation: dg-n4 7s ease-in-out infinite }
.dgLanding .anim .gnode.a5 { animation: dg-n5 7s ease-in-out infinite }
.dgLanding .anim .gnode.a6 { animation: dg-n6 7s ease-in-out infinite }

.dgLanding .anim .gedge.b1 { stroke-dasharray: 700; animation: dg-d1 7s linear infinite }
.dgLanding .anim .gedge.b2 { stroke-dasharray: 700; animation: dg-d2 7s linear infinite }
.dgLanding .anim .gedge.b3 { stroke-dasharray: 700; animation: dg-d3 7s linear infinite }
.dgLanding .anim .gedge.b4ai { animation: dg-aiedgein 7s ease-in-out infinite }
.dgLanding .anim .gedge.b5 { stroke-dasharray: 700; animation: dg-d5 7s linear infinite }
.dgLanding .anim .gedge.b6 { stroke-dasharray: 700; animation: dg-d6 7s linear infinite }

.dgLanding .anim .cflayer { animation: dg-redborder 7s ease-in-out infinite }
.dgLanding .anim .cfedge { animation: dg-cfedgein 7s ease-in-out infinite, dg-march 1.1s linear infinite }
.dgLanding .anim .warn { animation: dg-warnIn 7s ease-in-out infinite }
.dgLanding .anim .warn-ring { animation: dg-ring 7s ease-out infinite }
.dgLanding .anim .panel { animation: dg-panelIn 7s ease-in-out infinite }

.dgLanding .anim .lbl.cflbl {
  fill: #475569;
  font-weight: 600;
  animation: dg-cflabel 7s ease-in-out infinite;
}

@media (prefers-reduced-motion: reduce) {
  .dgLanding .anim *,
  .dgLanding .anim {
    animation: none !important;
  }

  .dgLanding .gnode,
  .dgLanding .gedge,
  .dgLanding .cflayer,
  .dgLanding .cfedge,
  .dgLanding .warn,
  .dgLanding .panel {
    opacity: 1 !important;
    transform: none !important;
    stroke-dashoffset: 0 !important;
  }

  .dgLanding .lbl.cflbl {
    fill: #dc2626 !important;
    font-weight: 700 !important;
  }

  .dgLanding .warn-ring {
    opacity: 0 !important;
  }
}

/* ---------- Responsive ---------- */
@media (max-width: 900px) {
  .dgLanding .hero .grid {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .dgLanding .hero h1 {
    font-size: 40px;
  }

  .dgLanding .steps,
  .dgLanding .values {
    grid-template-columns: 1fr;
  }

  .dgLanding .panel {
    width: 240px;
  }
}

@media (max-width: 560px) {
  .dgLanding .hero h1 {
    font-size: 32px;
  }

  .dgLanding .header-cta-text {
    display: none;
  }

  .dgLanding .panel {
    display: none;
  }
}
`;

export default function LandingPage() {
  const { isLoading, isLoggedIn } = useAuth();

  if (!isLoading && isLoggedIn) {
    return <Navigate to="/workspaces" replace />;
  }

  return (
    <div className="dgLanding">
      <style>{styles}</style>

      {/* ============ HEADER ============ */}
      <header className="site">
        <div className="wrap row">
          <a className="logo" href="#top">Doc<span>Graph</span></a>
          <a className="btn btn-primary" href={NOTION_LOGIN_URL}>
            <NotionMark className="notion-mark" />
            <span className="header-cta-text">Notion으로 시작하기</span>
          </a>
        </div>
      </header>

      {/* ============ HERO ============ */}
      <a id="top"></a>
      <section className="hero">
        <div className="wrap grid">
          {/* copy */}
          <div>
            <span className="eyebrow"><span className="dot"></span>AI 시대, 우리 팀 문서 관계를 한눈에</span>
            <h1>흩어진 문서를,<br /><span className="accent">하나의 그래프</span>로.</h1>
            <p className="sub">
              Notion 워크스페이스의 문서 연결 관계를 자동으로 추론해 그래프로 시각화하고,
              문서 간 정합성 충돌을 AI가 탐지·진단하고 수정까지 제안합니다.
            </p>
            <div className="cta">
              <a className="btn btn-primary btn-lg" href={NOTION_LOGIN_URL}>
                <NotionMark className="notion-mark" />
                Notion으로 시작하기
              </a>
              <a className="btn btn-ghost btn-lg" href="#how">자세히 알아보기</a>
            </div>
            <p className="trust">설치 없이 Notion 계정으로 바로 연동 · 워크스페이스 문서 그대로 사용</p>
          </div>

          {/* graph demo */}
          <div className="demo">
            <div className="canvas anim">
              {/* conflict panel mock */}
              <div className="panel" role="img" aria-label="정합성 충돌 패널 목업">
                <div className="ph">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="2"
                    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                    <line x1="12" y1="9" x2="12" y2="13" />
                    <line x1="12" y1="17" x2="12.01" y2="17" />
                  </svg>
                  <span className="ai-ttl">출시일 정의가 문서 간 불일치</span>
                </div>
                <div className="pbody">
                  <span className="lbl">충돌 원인 <span className="badge-ai">AI</span></span>
                  <div className="cause">
                    기획서는 출시일을 <b>7/1</b>로, 요구사항 명세서는 <b>6/15</b>로 기재하고 있어
                    두 문서의 출시일 정의가 서로 어긋납니다.
                  </div>
                  <div className="diff">
                    <div className="line del"><span className="sign">−</span><span>출시일: 2026-07-01</span></div>
                    <div className="line add"><span className="sign">+</span><span>출시일: 2026-06-15</span></div>
                  </div>
                </div>
                <div className="pacts">
                  <div className="pbtn sec">무시하기</div>
                  <div className="pbtn pri">제안 적용하기</div>
                </div>
              </div>

              {/* SVG graph */}
              <svg className="graph" viewBox="0 0 600 470" role="img" aria-label="문서 의존 그래프 데모">
                {/* ===== EDGES (behind nodes) ===== */}
                <g>
                  <line className="gedge b1" x1="95" y1="75" x2="80" y2="255" />{/* 기획서→설계 */}
                  <line className="gedge b2" x1="95" y1="75" x2="300" y2="250" />{/* 기획서→API */}
                  <line className="gedge b3" x1="430" y1="80" x2="300" y2="250" />{/* 요구사항→API */}
                  <line className="gedge ai b4ai" x1="430" y1="80" x2="470" y2="270" />{/* 요구사항⇢회의록 (AI 추론) */}
                  <line className="gedge b5" x1="300" y1="250" x2="470" y2="395" />{/* API→QA */}
                  <line className="gedge b6" x1="470" y1="270" x2="470" y2="395" />{/* 회의록→QA */}
                </g>

                {/* ===== CONFLICT EDGE (기획서 ↔ 요구사항) ===== */}
                <line className="cfedge" x1="95" y1="75" x2="430" y2="80" />

                {/* ===== NODES ===== */}
                {/* N1 기획서 (PRD) — conflict */}
                <g className="gnode a1 cf">
                  <rect className="card" x="67" y="47" width="56" height="56" rx="14" />
                  <g className="icn" transform="translate(83,63)">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <path d="M14 2v6h6" />
                    <path d="M16 13H8" />
                    <path d="M16 17H8" />
                    <path d="M10 9H8" />
                  </g>
                  <text className="lbl cflbl" x="95" y="120" textAnchor="middle">기획서 (PRD)</text>
                </g>

                {/* N2 요구사항 — conflict */}
                <g className="gnode a2 cf">
                  <rect className="card" x="402" y="52" width="56" height="56" rx="14" />
                  <g className="icn" transform="translate(418,68)">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <path d="M14 2v6h6" />
                    <path d="M9 13h6" />
                    <path d="M9 17h6" />
                    <path d="M9 9h1" />
                  </g>
                  <text className="lbl cflbl" x="430" y="125" textAnchor="middle">요구사항 명세서</text>
                </g>

                {/* N3 설계 */}
                <g className="gnode a3">
                  <rect className="card" x="52" y="227" width="56" height="56" rx="14" />
                  <g className="icn" transform="translate(68,243)">
                    <rect x="3" y="3" width="7" height="9" rx="1" />
                    <rect x="14" y="3" width="7" height="5" rx="1" />
                    <rect x="14" y="12" width="7" height="9" rx="1" />
                    <rect x="3" y="16" width="7" height="5" rx="1" />
                  </g>
                  <text className="lbl" x="80" y="300" textAnchor="middle">시스템 설계</text>
                </g>

                {/* N4 API 명세 */}
                <g className="gnode a4">
                  <rect className="card" x="272" y="222" width="56" height="56" rx="14" />
                  <g className="icn" transform="translate(288,238)">
                    <circle cx="18" cy="5" r="3" />
                    <circle cx="6" cy="12" r="3" />
                    <circle cx="18" cy="19" r="3" />
                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                  </g>
                  <text className="lbl" x="300" y="295" textAnchor="middle">API 명세</text>
                </g>

                {/* N5 회의록 */}
                <g className="gnode a5">
                  <rect className="card" x="442" y="242" width="56" height="56" rx="14" />
                  <g className="icn" transform="translate(458,258)">
                    <rect x="3" y="4" width="18" height="18" rx="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </g>
                  <text className="lbl" x="470" y="315" textAnchor="middle">회의록</text>
                </g>

                {/* N6 QA 검증 (회의록 아래) */}
                <g className="gnode a6">
                  <rect className="card" x="442" y="367" width="56" height="56" rx="14" />
                  <g className="icn" transform="translate(458,383)">
                    <rect x="8" y="2" width="8" height="4" rx="1" />
                    <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
                    <path d="m9 14 2 2 4-4" />
                  </g>
                  <text className="lbl" x="470" y="440" textAnchor="middle">QA 검증</text>
                </g>

                {/* ===== CONFLICT OVERLAYS (red borders + badges) ===== */}
                <g className="cflayer">
                  <rect x="67" y="47" width="56" height="56" rx="14" fill="rgba(220,38,38,.06)" stroke="#dc2626"
                    strokeWidth="2.5" />
                  <circle cx="121" cy="49" r="8" fill="#dc2626" /><text x="121" y="52.5" textAnchor="middle" fill="#fff"
                    fontSize="10" fontWeight="700">1</text>
                  <rect x="402" y="52" width="56" height="56" rx="14" fill="rgba(220,38,38,.06)" stroke="#dc2626"
                    strokeWidth="2.5" />
                  <circle cx="456" cy="54" r="8" fill="#dc2626" /><text x="456" y="57.5" textAnchor="middle" fill="#fff"
                    fontSize="10" fontWeight="700">1</text>
                </g>

                {/* ===== WARNING at conflict midpoint ===== */}
                <g transform="translate(262,77)">
                  <circle className="warn-ring" cx="0" cy="0" r="16" fill="none" stroke="#dc2626" strokeWidth="2" />
                  <g className="warn">
                    <circle cx="0" cy="0" r="15" fill="#fff" stroke="#fecaca" strokeWidth="2" />
                    <path d="M0 -8 L8 7 L-8 7 Z" fill="#dc2626" />
                    <rect x="-1" y="-3" width="2" height="5.5" rx="1" fill="#fff" />
                    <rect x="-1" y="4" width="2" height="2" rx="1" fill="#fff" />
                  </g>
                </g>
              </svg>

              {/* Legend */}
              <div className="legend">
                <h5>Graph Legend</h5>
                <div className="li"><span className="swatch solid"></span><span>Direct Dependency</span></div>
                <div className="li"><span className="swatch dashed"></span><span>AI Inference Link</span></div>
                <div className="li"><span className="swatch conflict"></span><span className="red">Conflict Detected</span></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ============ HOW IT WORKS ============ */}
      <section className="block bg-soft" id="how">
        <div className="wrap">
          <div className="sec-head">
            <div className="kicker">How it works</div>
            <h2>어떻게 동작하나요?</h2>
            <p>연동부터 충돌 감지까지, 세 단계면 충분합니다.</p>
          </div>
          <div className="steps">
            <div className="step">
              <span className="num">01</span>
              <div className="ic">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 17H7A5 5 0 0 1 7 7h2" />
                  <path d="M15 7h2a5 5 0 1 1 0 10h-2" />
                  <line x1="8" y1="12" x2="16" y2="12" />
                </svg>
              </div>
              <h3>Notion 워크스페이스 연결</h3>
              <p>Notion 계정으로 로그인하고 워크스페이스를 연동합니다. 프로젝트를 생성할 페이지 하나만 정해주세요.</p>
            </div>
            <div className="step">
              <span className="num">02</span>
              <div className="ic">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round">
                  <path d="m13.11 7.664 1.78 2.672" />
                  <path d="m14.162 12.788-3.324 1.424" />
                  <path d="m20 4-6.06 1.515" />
                  <path d="M3 3v16a2 2 0 0 0 2 2h16" />
                  <circle cx="12" cy="6" r="2" />
                  <circle cx="16" cy="12" r="2" />
                  <circle cx="9" cy="15" r="2" />
                </svg>
              </div>
              <h3>문서 의존 그래프 자동 생성</h3>
              <p>문서 간의 연결 관계를 자동으로 추론하고, 한눈에 보이는 의존 관계 그래프로 그려냅니다.</p>
            </div>
            <div className="step">
              <span className="num">03</span>
              <div className="ic">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round">
                  <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                  <line x1="12" y1="9" x2="12" y2="13" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
              </div>
              <h3>변경 시 충돌 감지 &amp; AI 수정 제안</h3>
              <p>문서가 바뀔 때마다 연결된 문서와의 정합성 충돌을 탐지하고, 원인 분석과 AI 수정안을 함께 제시합니다.</p>
            </div>
          </div>
        </div>
      </section>

      {/* ============ VALUES ============ */}
      <section className="block">
        <div className="wrap">
          <div className="sec-head">
            <div className="kicker">Why DocGraph?</div>
            <h2>문서가 늘어나도, 어긋나지 않게</h2>
            <p>연결을 모델링하고, 충돌을 검증하고, 익숙한 방식 그대로 사용하면 됩니다.</p>
          </div>
          <div className="values">
            <div className="vcard">
              <div className="ic">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="18" cy="5" r="3" />
                  <circle cx="6" cy="12" r="3" />
                  <circle cx="18" cy="19" r="3" />
                  <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                  <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                </svg>
              </div>
              <h3>자동 의존성 추론</h3>
              <p>문서 사이의 링크·멘션뿐만 아니라 단어까지 자동으로 분석해 연결 관계를 그래프로 모델링합니다. 숨어 있던 의존성까지 드러납니다.</p>
            </div>
            <div className="vcard">
              <div className="ic">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  <path d="m9 12 2 2 4-4" />
                </svg>
              </div>
              <h3>AI 정합성 검증</h3>
              <p>연결된 문서 쌍의 충돌을 탐지하고 원인을 분석합니다. 그리고 바로 적용할 수 있는 AI 수정안을 제시합니다.</p>
            </div>
            <div className="vcard">
              <div className="ic">
                <NotionMark width={22} height={22} />
              </div>
              <h3>Notion 그대로의 경험</h3>
              <p>쓰던 방식 그대로, 내 손이 향하는 곳에 문서가 있습니다. 언제든지 Notion과 DocGraph를 자유롭게 넘나들 수 있습니다.</p>
            </div>
          </div>
        </div>
      </section>

      {/* ============ CTA BAND ============ */}
      <section className="cta-band">
        <div className="wrap">
          <div className="cta-inner">
            <h2>지금 바로 시작하세요</h2>
            <p>Notion 계정만 있으면 1분 만에 내 워크스페이스의 의존 관계 그래프를 만날 수 있습니다.</p>
            <a className="btn btn-white" href={NOTION_LOGIN_URL}>
              <NotionMark className="notion-mark" />
              Notion으로 시작하기
            </a>
          </div>
        </div>
      </section>

      {/* ============ FOOTER ============ */}
      <footer className="site">
        <div className="wrap row">
          <div className="cp">© 2026 DocGraph.</div>
          <div className="links">
            <a href="#">개인정보 처리방침</a>
            <a href="#">이용약관</a>
            <a href="#">고객센터</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
