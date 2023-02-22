import classNames from "classnames";
import { useCallback, useRef, useState } from "react";
import React from "react";
import { useInView } from "react-intersection-observer";
import { useResizeDetector } from "react-resize-detector";

import { Tooltip } from "components/ui/Tooltip";

import { Text, TextProps } from "./Text";
import styles from "./TextWithOverflowTooltip.module.scss";

interface TextWithOverflowTooltipProps extends TextProps {
  onlyCheckForSizeIfInView?: boolean;
}
export const TextWithOverflowTooltip: React.FC<React.PropsWithChildren<TextWithOverflowTooltipProps>> = React.memo(
  ({ children, onlyCheckForSizeIfInView = false, className, ...props }) => {
    const [tooltipDisabled, setTooltipDisabled] = useState(true);

    const textRef = useRef<HTMLElement | null>(null);
    const { inView, ref: inViewRef } = useInView({ delay: 1000, skip: !onlyCheckForSizeIfInView });

    const onResize = useCallback(() => {
      if (!onlyCheckForSizeIfInView || inView) {
        setTooltipDisabled((textRef.current?.scrollWidth ?? 0) <= (textRef.current?.clientWidth ?? 0));
      }
    }, [inView, onlyCheckForSizeIfInView]);

    const { ref } = useResizeDetector<HTMLElement>({
      onResize,
      refreshMode: "debounce",
      handleHeight: false,
      handleWidth: !onlyCheckForSizeIfInView || inView,
    });

    return (
      <Tooltip
        control={
          <Text
            ref={(element) => {
              textRef.current = element;
              ref.current = element;
              inViewRef(element);
            }}
            className={classNames(className, styles.overflowText)}
            {...props}
          >
            {children}
          </Text>
        }
        disabled={tooltipDisabled}
      >
        {textRef.current?.textContent}
      </Tooltip>
    );
  }
);
