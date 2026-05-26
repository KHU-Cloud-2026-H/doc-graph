import React from 'react';
import { Check } from 'lucide-react';

const STEP_LABELS = ['프로젝트 생성', '카테고리 등록', '프로젝트 멤버 배정', '카테고리별 담당자 배정'] as const;

interface StepIndicatorProps {
  currentStep: number;
  totalSteps: number;
}

const StepIndicator: React.FC<StepIndicatorProps> = ({ currentStep, totalSteps }) => {
  const steps = Array.from({ length: totalSteps }, (_, i) => i + 1);

  return (
    <div>
      <p className="text-xl font-semibold text-gray-800 mb-6">새 프로젝트 생성</p>
      <div className="flex flex-col">
        {steps.map((stepNum, idx) => (
          <React.Fragment key={stepNum}>
            <div className="flex items-center gap-3">
              {stepNum < currentStep ? (
                <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center shrink-0">
                  <Check size={16} className="text-white" strokeWidth={2.5} />
                </div>
              ) : stepNum === currentStep ? (
                <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center shrink-0">
                  <span className="text-white text-sm font-semibold">{stepNum}</span>
                </div>
              ) : (
                <div className="w-8 h-8 rounded-full bg-white border-2 border-gray-300 flex items-center justify-center shrink-0">
                  <span className="text-gray-400 text-sm font-semibold">{stepNum}</span>
                </div>
              )}
              <span
                className={`text-sm font-medium ${
                  stepNum <= currentStep ? 'text-blue-600' : 'text-gray-400'
                }`}
              >
                {STEP_LABELS[idx]}
              </span>
            </div>
            {idx < steps.length - 1 && (
              <div
                className={`h-8 w-0.5 ml-[15px] ${
                  stepNum < currentStep ? 'bg-blue-500' : 'bg-gray-300'
                }`}
              />
            )}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
};

export default StepIndicator;
